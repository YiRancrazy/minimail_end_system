package com.yirancrazy.minimall.notify.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.ComplaintStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.service.ComplaintService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉领域服务实现，实现投诉提交、分页查询、处理业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintManager complaintManager;

    public ComplaintServiceImpl(ComplaintManager complaintManager) {
        this.complaintManager = complaintManager;
    }

    /**
     * 提交投诉，使用 complainantId 覆盖 DTO 中的投诉方ID，初始状态为 PENDING。
     * @param complainantId 投诉方ID，来自可信 Header
     * @param dto 创建入参
     * @return 新投诉ID
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
        log.info("complaint created, id={}, complainantId={}", po.getId(), complainantId);
        return po.getId();
    }

    /**
     * 平台分页查询投诉，支持按状态和订单号过滤，按 ID 降序返回。
     * @param dto 分页入参
     * @return 投诉分页结果
     */
    @Override
    public IPage<ComplaintPO> page(ComplaintPageDTO dto) {
        Page<ComplaintPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        return complaintManager.page(page, Wrappers.lambdaQuery(ComplaintPO.class)
            .eq(dto.getStatus() != null, ComplaintPO::getStatus, dto.getStatus())
            .like(dto.getOrderNo() != null && !dto.getOrderNo().isBlank(),
                ComplaintPO::getOrderNo, dto.getOrderNo())
            .orderByDesc(ComplaintPO::getId));
    }

    /**
     * 平台处理投诉，校验状态流转：PENDING→PROCESSING，PROCESSING→RESOLVED/REJECTED。
     * @param id 投诉ID
     * @param handlerId 处理人ID
     * @param dto 处理入参
     * @throws BizException 投诉不存在或状态流转非法时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, Long handlerId, ComplaintHandleDTO dto) {
        ComplaintPO po = complaintManager.getById(id);
        if (po == null) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_NOT_FOUND);
        }
        if (!isValidTransition(po.getStatus(), dto.getStatus())) {
            throw new BizException(NotifyCodeEnum.COMPLAINT_STATUS_INVALID);
        }
        po.setStatus(dto.getStatus());
        po.setHandlerId(handlerId);
        po.setHandlerResult(dto.getResult());
        complaintManager.updateById(po);
        log.info("complaint handled, id={}, handlerId={}, newStatus={}", id, handlerId, dto.getStatus());
    }

    private boolean isValidTransition(Integer current, Integer target) {
        if (current == null || target == null) {
            return false;
        }
        // PENDING → PROCESSING
        if (current == ComplaintStatusEnum.PENDING.intCode()
            && target == ComplaintStatusEnum.PROCESSING.intCode()) {
            return true;
        }
        // PROCESSING → RESOLVED
        if (current == ComplaintStatusEnum.PROCESSING.intCode()
            && target == ComplaintStatusEnum.RESOLVED.intCode()) {
            return true;
        }
        // PROCESSING → REJECTED
        if (current == ComplaintStatusEnum.PROCESSING.intCode()
            && target == ComplaintStatusEnum.REJECTED.intCode()) {
            return true;
        }
        return false;
    }
}
