package com.yirancrazy.minimall.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.notify.constant.ComplaintStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDto;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDto;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDto;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.service.ComplaintService;
import com.yirancrazy.minimall.notify.vo.ComplaintVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 负责投诉创建、游标查询、用户归属校验和平台处理状态机；处理更新以原状态作为并发条件，避免重复推进.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintManager complaintManager;

    /**
     * 注入投诉记录的数据访问入口.
     *
     * @param complaintManager 投诉记录持久化入口
     */
    public ComplaintServiceImpl(ComplaintManager complaintManager) {
        this.complaintManager = complaintManager;
    }

    /**
     * 创建状态为待处理的投诉，并使用可信请求头中的投诉人编号覆盖请求体字段.
     *
     * @param complainantId 当前登录用户编号
     * @param dto 投诉双方、关联订单、类型、标题和正文
     * @param idempotencyKey 格式化后的投诉幂等键
     * @return 新投诉主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long complainantId, ComplaintCreateDto dto, String idempotencyKey) {
        ComplaintPo po = new ComplaintPo();
        po.setComplainantType(dto.getComplainantType());
        po.setComplainantId(complainantId);
        po.setDefendantType(dto.getDefendantType());
        po.setDefendantId(dto.getDefendantId());
        po.setOrderNo(dto.getOrderNo());
        po.setComplaintType(dto.getComplaintType());
        po.setTitle(dto.getTitle());
        po.setContent(dto.getContent());
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        po.setIdempotencyKey(idempotencyKey);
        complaintManager.save(po);
        log.info("投诉已创建，id={}，complainantId={}", po.getId(), complainantId);
        return po.getId();
    }

    /**
     * 使用主键游标倒序查询投诉，并支持状态、订单号模糊匹配、投诉人和被投诉人过滤.
     *
     * @param dto 游标及投诉筛选条件
     * @return 当前页投诉实体及下一页游标
     */
    public CursorPageVo<ComplaintPo> page(ComplaintPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        final int limit = dto.getLimit();
        LambdaQueryWrapper<ComplaintPo> wrapper = Wrappers.lambdaQuery(ComplaintPo.class);
        wrapper.lt(lastId != null, ComplaintPo::getId, lastId);
        if (dto.getStatus() != null) {
            wrapper.eq(ComplaintPo::getStatus, dto.getStatus());
        }
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            wrapper.like(ComplaintPo::getOrderNo, dto.getOrderNo());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(ComplaintPo::getComplainantId, dto.getUserId());
        }
        if (dto.getDefendantId() != null) {
            wrapper.eq(ComplaintPo::getDefendantId, dto.getDefendantId());
        }
        wrapper.orderByDesc(ComplaintPo::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<ComplaintPo> records = complaintManager.list(wrapper);
        return CursorPageVo.of(records, limit, ComplaintPo::getId);
    }

    /**
     * 将游标查询得到的投诉实体转换为对外视图.
     *
     * @param dto 游标及投诉筛选条件
     * @return 当前页投诉视图及下一页游标
     */
    @Override
    public CursorPageVo<ComplaintVo> pageVo(ComplaintPageDto dto) {
        return page(dto).map(ComplaintVo::from);
    }

    /**
     * 在事务内校验投诉状态流转，并以原状态作为条件更新状态、处理人和处理结果；并发更新失败按状态非法处理.
     *
     * @param id 投诉主键
     * @param handlerId 平台处理人编号
     * @param dto 目标状态和处理结果
     * @throws BizException 投诉不存在、流转不合法或条件更新影响零行时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Long handlerId, ComplaintHandleDto dto) {
        ComplaintPo po = complaintManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        if (!isValidTransition(po.getStatus(), dto.getStatus())) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_STATUS_INVALID);
        }
        boolean updated =
                complaintManager.update(
                        Wrappers.lambdaUpdate(ComplaintPo.class)
                                .eq(ComplaintPo::getId, id)
                                .eq(ComplaintPo::getStatus, po.getStatus())
                                .set(ComplaintPo::getStatus, dto.getStatus())
                                .set(ComplaintPo::getHandlerId, handlerId)
                                .set(ComplaintPo::getHandlerResult, dto.getResult()));
        if (!updated) {
            // 条件更新 0 行：并发下状态已被其他请求推进，按状态流转非法处理
            throw new BizException(NotifyCodeEnum.COMPLAINT_STATUS_INVALID);
        }
        log.info("投诉已处理，id={}，handlerId={}，newStatus={}", id, handlerId, dto.getStatus());
    }

    /**
     * 按主键查询投诉实体，不存在时抛出业务异常.
     *
     * @param id 投诉主键
     * @return 投诉实体
     * @throws BizException 投诉不存在时抛出
     */
    public ComplaintPo detail(Long id) {
        ComplaintPo po = complaintManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        return po;
    }

    /**
     * 查询投诉实体并转换为平台侧投诉视图.
     *
     * @param id 投诉主键
     * @return 投诉视图
     * @throws BizException 投诉不存在时抛出
     */
    @Override
    public ComplaintVo detailVo(Long id) {
        return ComplaintVo.from(detail(id));
    }

    /**
     * 查询并返回当前用户作为投诉人的投诉；非本人记录统一按不存在处理，避免泄露记录是否存在.
     *
     * @param id 投诉主键
     * @param userId 当前登录用户编号
     * @return 用户侧投诉视图
     * @throws BizException 投诉不存在或投诉人不是当前用户时抛出
     */
    @Override
    public ComplaintVo detailForUser(Long id, Long userId) {
        ComplaintPo po = detail(id);
        if (po.getComplainantId() == null || !po.getComplainantId().equals(userId)) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        return ComplaintVo.from(po);
    }

    /**
     * 判断投诉是否允许从当前状态流转到目标状态；仅支持待处理到处理中、处理中到已解决或已驳回.
     *
     * @param current 当前状态编码
     * @param target 目标状态编码
     * @return true 表示状态流转合法
     */
    private boolean isValidTransition(Integer current, Integer target) {
        if (current == null || target == null) {
            return false;
        }
        // PENDING 转 PROCESSING
        if (current == ComplaintStatusEnum.PENDING.intCode()
                && target == ComplaintStatusEnum.PROCESSING.intCode()) {
            return true;
        }
        // PROCESSING 转 RESOLVED
        if (current == ComplaintStatusEnum.PROCESSING.intCode()
                && target == ComplaintStatusEnum.RESOLVED.intCode()) {
            return true;
        }
        // PROCESSING 转 REJECTED
        if (current == ComplaintStatusEnum.PROCESSING.intCode()
                && target == ComplaintStatusEnum.REJECTED.intCode()) {
            return true;
        }
        return false;
    }
}
