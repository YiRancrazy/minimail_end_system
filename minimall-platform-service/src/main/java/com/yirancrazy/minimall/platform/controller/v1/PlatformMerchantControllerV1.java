package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家控制器，提供商家列表与详情接口。
 *              当前阶段数据由 merchant-service 提供，本端作为契约入口。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/platform/merchants/manage")
public class PlatformMerchantControllerV1 {

    private final PlatformMerchantService platformMerchantService;

    public PlatformMerchantControllerV1(PlatformMerchantService platformMerchantService) {
        this.platformMerchantService = platformMerchantService;
    }

    /**
     * 平台分页查询商家列表。
     * @param dto 分页入参
     * @return 商家游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<PlatformMerchantVO>> page(@Valid PlatformMerchantPageDTO dto) {
        return Result.success(platformMerchantService.page(dto));
    }

    /**
     * 平台查询商家详情。
     * @param merchantId 商家ID
     * @return 商家视图
     */
    @GetMapping("/{merchantId}")
    public Result<PlatformMerchantVO> detail(@PathVariable("merchantId") Long merchantId) {
        return Result.success(platformMerchantService.detail(merchantId));
    }
}
