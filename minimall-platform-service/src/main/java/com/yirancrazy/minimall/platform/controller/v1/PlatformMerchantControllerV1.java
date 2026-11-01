package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDto;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端商家管理接口，提供商家列表与详情查询，并委派服务层访问商家数据.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/merchants/manage")
public class PlatformMerchantControllerV1 {

    private final PlatformMerchantService platformMerchantService;

    /**
     * 注入平台商家服务，构造商家管理接口.
     *
     * @param platformMerchantService 平台商家业务服务
     */
    public PlatformMerchantControllerV1(PlatformMerchantService platformMerchantService) {
        this.platformMerchantService = platformMerchantService;
    }

    /**
     * 按游标、关键词及审核状态查询商家管理列表.
     *
     * @param dto 商家分页筛选条件
     * @return 商家游标分页数据
     */
    @GetMapping
    public Result<CursorPageVo<PlatformMerchantVo>> page(@Valid PlatformMerchantPageDto dto) {
        return Result.success(platformMerchantService.page(dto));
    }

    /**
     * 查询指定商家的管理详情.
     *
     * @param merchantId 商家 ID
     * @return 商家管理详情
     * @throws com.yirancrazy.minimall.common.exception.BizException 商家服务未返回商家数据时抛出
     */
    @GetMapping("/{merchantId}")
    public Result<PlatformMerchantVo> detail(@PathVariable("merchantId") Long merchantId) {
        return Result.success(platformMerchantService.detail(merchantId));
    }
}
