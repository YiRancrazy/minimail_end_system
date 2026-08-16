package com.yirancrazy.minimall.merchant.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVO;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质控制器，提供资质提交、查询与平台审核 RESTful API。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant/merchants")
public class MerchantControllerV1 {

    private final MerchantService merchantService;

    public MerchantControllerV1(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 商家提交或更新资质，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 资质提交入参
     * @return 资质VO
     */
    @PostMapping("/qualification")
    public Result<MerchantQualificationVO> submitQualification(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                               @Valid @RequestBody QualificationSubmitDTO dto) {
        return Result.success(merchantService.submitQualification(merchantId, dto));
    }

    /**
     * 查询商家资质状态。
     * @param merchantId 商家ID
     * @return 资质VO
     */
    @GetMapping("/qualification")
    public Result<MerchantQualificationVO> getQualification(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(merchantService.getQualification(merchantId));
    }

    /**
     * 平台审核商家资质，仅允许待审核状态审核；非 PLATFORM 角色一律拒绝。
     * @param id 商家主体ID
     * @param approved 是否通过
     * @param reason 驳回原因
     * @param role 网关注入的角色码（X-User-Role 头）
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN
     */
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @RequestParam boolean approved,
                              @RequestParam(required = false) String reason,
                              @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        merchantService.audit(id, approved, reason);
        return Result.success(null);
    }

    /**
     * 商家获取自身信息，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @return 商家信息VO
     */
    @GetMapping("/me")
    public Result<MerchantInfoVO> getMerchantInfo(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(merchantService.getMerchantInfo(merchantId));
    }

    /**
     * 商家查看审核记录，包含资质审核与商品审核日志。
     * @param merchantId 商家ID
     * @return 审核记录列表
     */
    @GetMapping("/audit-log")
    public Result<List<MerchantAuditLogVO>> listAuditLog(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(merchantService.listAuditLog(merchantId));
    }

    /**
     * 校验当前角色为平台管理员；网关按 JWT role claim 注入 X-User-Role 头，客户端伪造头会被网关移除。
     * @param role 网关注入的角色码
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN(20003)
     */
    private void requirePlatform(String role) {
        if (!RoleEnum.PLATFORM.getCode().equals(role)) {
            throw new BizException(CommonCode.FORBIDDEN, "FORBIDDEN", "无权限访问");
        }
    }
}
