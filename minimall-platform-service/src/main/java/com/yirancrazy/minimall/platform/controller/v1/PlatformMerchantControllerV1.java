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
 * @Description: PlatformMerchantControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/merchants/manage")
public class PlatformMerchantControllerV1 {

    private final PlatformMerchantService platformMerchantService;

    /**
     * 构造 PlatformMerchantControllerV1 实例。
     * @param platformMerchantService platformMerchantService 参数
     */
    public PlatformMerchantControllerV1(PlatformMerchantService platformMerchantService) {
        this.platformMerchantService = platformMerchantService;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<PlatformMerchantVO>> page(@Valid PlatformMerchantPageDTO dto) {
        return Result.success(platformMerchantService.page(dto));
    }


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{merchantId}")
    public Result<PlatformMerchantVO> detail(@PathVariable("merchantId") Long merchantId) {
        return Result.success(platformMerchantService.detail(merchantId));
    }
}
