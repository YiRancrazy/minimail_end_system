package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.service.MerchantService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家管理内部控制器（仅内网调用），供平台服务远程调用。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/internal/merchant/manage")
public class InternalMerchantManageControllerV1 {

    private final MerchantService merchantService;

    public InternalMerchantManageControllerV1(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 平台分页查询商家列表。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param query 通用分页查询
     * @return 商家管理分页结果
     */
    @GetMapping
    public Result<CursorPageVO<MerchantManageVO>> page(InternalPageQuery query) {
        MerchantPageDTO dto = new MerchantPageDTO();
        dto.setCursor(query.getCursor());
        dto.setLimit(query.getLimit() == null ? 20 : query.getLimit());
        dto.setKeyword(query.getKeyword());
        dto.setAuditStatus(query.getStatus());
        CursorPageVO<MerchantPO> po = merchantService.page(dto);
        return Result.success(po.map(this::toVO));
    }

    /**
     * 平台查询商家详情。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param merchantId 商家ID
     * @return 商家管理视图
     */
    @GetMapping("/{merchantId}")
    public Result<MerchantManageVO> detail(@PathVariable("merchantId") Long merchantId) {
        return Result.success(toVO(merchantService.detail(merchantId)));
    }

    /**
     * 平台审核商家资质，仅允许 PENDING 状态审核；驳回时 reason 必填。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param merchantId 商家主体ID
     * @param approved 是否通过
     * @param reason 驳回原因，approved=false 时必填
     * @return 空成功响应
     */
    @PostMapping("/{merchantId}/audit")
    public Result<Void> audit(@PathVariable("merchantId") Long merchantId,
                              @RequestParam boolean approved,
                              @RequestParam(required = false) String reason) {
        if (!approved && (reason == null || reason.isBlank())) {
            throw new BizException(CommonCode.PARAM_INVALID, "PARAM_INVALID", "驳回时原因不能为空");
        }
        merchantService.audit(merchantId, approved, reason);
        return Result.success(null);
    }

    /**
     * PO → VO 转换。
     * @param p 商家持久化对象
     * @return 商家管理视图
     */
    private MerchantManageVO toVO(MerchantPO p) {
        return new MerchantManageVO(p.getId(), p.getUserId(), p.getMerchantName(),
                p.getLicenseNo(), p.getLicenseImageUrl(), p.getAuditStatus(), p.getAuditReason(),
                p.getAuditAt(), p.getCreateTime());
    }
}
