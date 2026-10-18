package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDTO;
import com.yirancrazy.minimall.merchant.service.MerchantService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalMerchantManageControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/merchant/manage")
public class InternalMerchantManageControllerV1 {

    private final MerchantService merchantService;

    /**
     * 构造 InternalMerchantManageControllerV1 实例。
     * @param merchantService merchantService 参数
     */
    public InternalMerchantManageControllerV1(MerchantService merchantService) {
        this.merchantService = merchantService;
    }


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<MerchantManageVO>> page(InternalPageQuery query) {
        MerchantPageDTO dto = new MerchantPageDTO();
        dto.setCursor(query.getCursor());
        dto.setLimit(query.getLimit() == null ? 20 : query.getLimit());
        dto.setKeyword(query.getKeyword());
        dto.setAuditStatus(query.getStatus());
        return Result.success(merchantService.page(dto));
    }


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{merchantId}")
    public Result<MerchantManageVO> detail(@PathVariable("merchantId") Long merchantId) {
        return Result.success(merchantService.detail(merchantId));
    }


    /**
     * 执行 audit 方法。
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @param reason reason 参数
     * @return 统一响应结果
     */
    @PostMapping("/{merchantId}/audit")
    public Result<Void> audit(@PathVariable("merchantId") Long merchantId,
                              @RequestParam boolean approved,
                              @RequestParam(required = false) String reason) {
        merchantService.audit(merchantId, approved, reason);
        return Result.success(null);
    }
}
