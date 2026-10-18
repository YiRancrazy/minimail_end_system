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
 * @Description: MerchantControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant/merchants")
public class MerchantControllerV1 {

    private final MerchantService merchantService;

    /**
     * 构造 MerchantControllerV1 实例。
     * @param merchantService merchantService 参数
     */
    public MerchantControllerV1(MerchantService merchantService) {
        this.merchantService = merchantService;
    }


    /**
     * 执行 submitQualification 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/qualification")
    public Result<MerchantQualificationVO> submitQualification(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                               @Valid @RequestBody QualificationSubmitDTO dto) {
        return Result.success(merchantService.submitQualification(merchantId, dto));
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/qualification")
    public Result<MerchantQualificationVO> getQualification(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(merchantService.getQualification(merchantId));
    }


    /**
     * 执行 audit 方法。
     * @param id id 参数
     * @param approved approved 参数
     * @param reason reason 参数
     * @param role role 参数
     * @return 统一响应结果
     */
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @RequestParam boolean approved,
                              @RequestParam(required = false) String reason,
                              @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        if (!approved && (reason == null || reason.isBlank())) {
            throw new BizException(CommonCode.PARAM_INVALID, "PARAM_INVALID", "驳回时原因不能为空");
        }
        merchantService.audit(id, approved, reason);
        return Result.success(null);
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/me")
    public Result<MerchantInfoVO> getMerchantInfo(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(merchantService.getMerchantInfo(merchantId));
    }


    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/audit-log")
    public Result<List<MerchantAuditLogVO>> listAuditLog(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(merchantService.listAuditLog(merchantId));
    }


    /**
     * 处理require平台。
     * @param role 角色
     * @throws BizException 业务校验失败时抛出
     */
    private void requirePlatform(String role) {
        if (!RoleEnum.PLATFORM.getCode().equals(role)) {
            throw new BizException(CommonCode.FORBIDDEN, "FORBIDDEN", "无权限访问");
        }
    }
}
