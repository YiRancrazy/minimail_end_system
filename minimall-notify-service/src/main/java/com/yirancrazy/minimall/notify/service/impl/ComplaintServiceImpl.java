package com.yirancrazy.minimall.notify.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.notify.constant.ComplaintStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.service.ComplaintService;
import com.yirancrazy.minimall.notify.vo.ComplaintVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintManager complaintManager;

    /**
     * 构造 ComplaintServiceImpl 实例。
     * @param complaintManager complaintManager 参数
     */
    public ComplaintServiceImpl(ComplaintManager complaintManager) {
        this.complaintManager = complaintManager;
    }


    /**
     * 创建业务数据。
     * @param complainantId complainantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long complainantId, ComplaintCreateDTO dto) {
        ComplaintPO po = new ComplaintPO();
        po.setComplainantType(dto.getComplainantType());
        po.setComplainantId(complainantId);
        po.setDefendantType(dto.getDefendantType());
        po.setDefendantId(dto.getDefendantId());
        po.setOrderNo(dto.getOrderNo());
        po.setComplaintType(dto.getComplaintType());
        po.setTitle(dto.getTitle());
        po.setContent(dto.getContent());
        po.setStatus(ComplaintStatusEnum.PENDING.intCode());
        complaintManager.save(po);
        log.info("投诉已创建，id={}，complainantId={}", po.getId(), complainantId);
        return po.getId();
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<ComplaintPO> page(ComplaintPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        LambdaQueryWrapper<ComplaintPO> wrapper = Wrappers.lambdaQuery(ComplaintPO.class);
        wrapper.lt(lastId != null, ComplaintPO::getId, lastId);
        if (dto.getStatus() != null) {
            wrapper.eq(ComplaintPO::getStatus, dto.getStatus());
        }
        if (dto.getOrderNo() != null && !dto.getOrderNo().isBlank()) {
            wrapper.like(ComplaintPO::getOrderNo, dto.getOrderNo());
        }
        if (dto.getUserId() != null) {
            wrapper.eq(ComplaintPO::getComplainantId, dto.getUserId());
        }
        if (dto.getDefendantId() != null) {
            wrapper.eq(ComplaintPO::getDefendantId, dto.getDefendantId());
        }
        wrapper.orderByDesc(ComplaintPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<ComplaintPO> records = complaintManager.list(wrapper);
        return CursorPageVO.of(records, limit, ComplaintPO::getId);
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<ComplaintVO> pageVO(ComplaintPageDTO dto) {
        return page(dto).map(ComplaintVO::from);
    }


    /**
     * 处理业务事件。
     * @param id id 参数
     * @param handlerId handlerId 参数
     * @param dto dto 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Long handlerId, ComplaintHandleDTO dto) {
        ComplaintPO po = complaintManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        if (!isValidTransition(po.getStatus(), dto.getStatus())) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_STATUS_INVALID);
        }
        boolean updated = complaintManager.update(Wrappers.lambdaUpdate(ComplaintPO.class)
            .eq(ComplaintPO::getId, id)
            .eq(ComplaintPO::getStatus, po.getStatus())
            .set(ComplaintPO::getStatus, dto.getStatus())
            .set(ComplaintPO::getHandlerId, handlerId)
            .set(ComplaintPO::getHandlerResult, dto.getResult()));
        if (!updated) {
            // 条件更新 0 行：并发下状态已被其他请求推进，按状态流转非法处理
            throw new BizException(NotifyCodeEnum.COMPLAINT_STATUS_INVALID);
        }
        log.info("投诉已处理，id={}，handlerId={}，newStatus={}", id, handlerId, dto.getStatus());
    }


    /**
     * 执行 detail 方法。
     * @param id id 参数
     * @return 处理结果
     */
    public ComplaintPO detail(Long id) {
        ComplaintPO po = complaintManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        return po;
    }


    /**
     * 执行 detailVO 方法。
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public ComplaintVO detailVO(Long id) {
        return ComplaintVO.from(detail(id));
    }


    /**
     * 执行 detailForUser 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public ComplaintVO detailForUser(Long id, Long userId) {
        ComplaintPO po = detail(id);
        if (po.getComplainantId() == null || !po.getComplainantId().equals(userId)) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        return ComplaintVO.from(po);
    }

    /**
     * 校验ValidTransition。
     * @param current 当前
     * @param target 目标对象
     * @return 是否满足条件
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
