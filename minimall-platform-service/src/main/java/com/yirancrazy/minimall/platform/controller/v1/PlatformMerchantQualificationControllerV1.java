package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDto;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端商家资质审核接口，提供待审核资质查询与审核提交能力.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/merchants")
public class PlatformMerchantQualificationControllerV1 {

    private final PlatformMerchantService platformMerchantService;

    /**
     * 注入平台商家服务，构造资质审核接口.
     *
     * @param platformMerchantService 平台商家业务服务
     */
    public PlatformMerchantQualificationControllerV1(
            PlatformMerchantService platformMerchantService) {
        this.platformMerchantService = platformMerchantService;
    }

    /**
     * 查询待审核商家资质游标分页，审核状态由服务层固定为待审核.
     *
     * @param cursor 上一页游标，为空时从首条开始
     * @param limit 每页数量，默认 20
     * @return 待审核资质分页数据
     */
    @GetMapping("/qualifications")
    public Result<CursorPageVo<MerchantQualificationVo>> listPending(
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
        return Result.success(platformMerchantService.listPendingQualifications(cursor, limit));
    }

    /**
     * 提交商家资质审核结果；审核通过或拒绝均由请求体中的 approved 表示.
     *
     * @param merchantId 商家 ID
     * @param dto 资质审核请求
     * @return 成功响应，不携带数据
     * @throws com.yirancrazy.minimall.common.exception.BizException 商家服务拒绝或不可用时抛出
     */
    @PostMapping("/{merchantId}/qualifications/audit")
    public Result<Void> audit(
            @PathVariable Long merchantId, @Valid @RequestBody MerchantQualificationAuditDto dto) {
        platformMerchantService.auditQualification(merchantId, dto);
        return Result.success(null);
    }
}
