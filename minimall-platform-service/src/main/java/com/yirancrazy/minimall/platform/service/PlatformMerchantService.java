package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家服务接口；当前契约由 merchant-service 提供，本端作为契约入口。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
public interface PlatformMerchantService {

    /**
     * 分页查询商家列表，支持按名称/审核状态过滤。
     * @param dto 分页入参
     * @return 商家游标分页结果
     */
    CursorPageVO<PlatformMerchantVO> page(PlatformMerchantPageDTO dto);

    /**
     * 查询商家详情，不存在时抛 PLATFORM_MERCHANT_NOT_FOUND。
     * @param merchantId 商家ID
     * @return 商家视图
     */
    PlatformMerchantVO detail(Long merchantId);
}
