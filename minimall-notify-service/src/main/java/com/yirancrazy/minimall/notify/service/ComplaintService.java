package com.yirancrazy.minimall.notify.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉领域服务接口，定义投诉提交、分页查询、处理业务契约
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public interface ComplaintService {

    /**
     * 提交投诉，使用 complainantId 覆盖 DTO 中的投诉方ID。
     * @param complainantId 投诉方ID，来自 X-User-Id Header
     * @param dto 创建入参
     * @return 新投诉ID
     */
    Long create(Long complainantId, ComplaintCreateDTO dto);

    /**
     * 平台分页查询投诉，支持按状态和订单号过滤。
     * @param dto 分页入参
     * @return 投诉分页结果
     */
    IPage<ComplaintPO> page(ComplaintPageDTO dto);

    /**
     * 平台处理投诉，校验状态流转合法性：PENDING→PROCESSING，PROCESSING→RESOLVED/REJECTED。
     * @param id 投诉ID
     * @param handlerId 处理人ID，来自 X-User-Id Header
     * @param dto 处理入参
     * @throws com.yirancrazy.minimall.common.exception.BizException 投诉不存在或状态非法时
     */
    void handle(Long id, Long handlerId, ComplaintHandleDTO dto);
}
