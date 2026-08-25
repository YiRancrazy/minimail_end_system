package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家资质审核控制器。审核动作经 MerchantFeignClient 委托 merchant-service 真实落库；
 *              listPending 仍为契约占位（数据由 merchant-service 提供）。
 * @Version: 2.0
 * @DateTime: 2026/08/16
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/merchants")
@RequiredArgsConstructor
public class PlatformMerchantQualificationControllerV1 {

    private final MerchantFeignClient merchantFeignClient;

    /**
     * 平台分页查询待审核商家资质。
     * 数据经 MerchantFeignClient 委托 merchant-service 返回 PENDING 状态商家，再映射为资质视图。
     * @param cursor 游标
     * @param limit  每页条数
     * @return 待审核资质分页结果
     */
    @GetMapping("/qualifications")
    public Result<CursorPageVO<MerchantQualificationVO>> listPending(@RequestParam(required = false) String cursor,
                                                                    @RequestParam(defaultValue = "20") int limit) {
        InternalPageQuery query = new InternalPageQuery();
        query.setCursor(cursor);
        query.setLimit(limit);
        // 商家资质待审核码：MerchantAuditStatusEnum.PENDING.code = 0
        query.setStatus(0);
        Result<CursorPageVO<MerchantManageVO>> result = merchantFeignClient.pageManage(query);
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            log.warn("list pending merchant qualifications failed, cursor={}, limit={}", cursor, limit);
            throw new BizException(CommonCode.SYS_ERROR, "商家服务处理失败");
        }
        CursorPageVO<MerchantManageVO> page = result.getData();
        return Result.success(page.map(this::toQualificationVO));
    }

    /**
     * 商家管理 VO 映射为资质视图（merchantId 与 userId 等值；法人/经营范围等字段未在商家管理契约中暴露）。
     * @param manage 商家管理VO
     * @return 资质视图
     */
    private MerchantQualificationVO toQualificationVO(MerchantManageVO manage) {
        MerchantQualificationVO vo = new MerchantQualificationVO();
        vo.setId(manage.getMerchantId());
        vo.setMerchantId(manage.getMerchantId());
        vo.setMerchantName(manage.getMerchantName());
        vo.setLicenseNo(manage.getLicenseNo());
        vo.setStatus(auditStatusName(manage.getAuditStatus()));
        vo.setRejectReason(manage.getAuditReason());
        vo.setSubmitTime(manage.getCreateTime());
        vo.setAuditTime(manage.getAuditAt());
        return vo;
    }

    /**
     * 审核状态码映射为枚举名（MerchantAuditStatusEnum：0-待审核, 1-已通过, 2-已驳回）。
     * @param code 审核状态码
     * @return 枚举名；未知码返回 REJECTED 前原样数值字符串
     */
    private String auditStatusName(Integer code) {
        if (code == null) return "PENDING";
        switch (code) {
            case 0: return "PENDING";
            case 1: return "APPROVED";
            case 2: return "REJECTED";
            default: return String.valueOf(code);
        }
    }

    /**
     * 平台审核商家资质，委托 merchant-service 校验 PENDING 状态并落库；
     * 远端失败（含服务不可用）时抛出 SYS_ERROR，不再静默返回成功。
     * @param merchantId 商家ID
     * @param dto 审核入参
     * @return 空成功响应
     * @throws BizException 远端审核失败或服务不可用时
     */
    @PostMapping("/{merchantId}/qualifications/audit")
    public Result<Void> audit(@PathVariable Long merchantId,
                               @Valid @RequestBody MerchantQualificationAuditDTO dto) {
        log.info("platform audit merchant qualification, merchantId={}, qualificationId={}, approved={}, reason={}",
                merchantId, dto.getQualificationId(), dto.getApproved(), dto.getReason());
        Result<Void> r = merchantFeignClient.audit(merchantId, dto.getApproved(), dto.getReason());
        if (r == null || !CommonCode.SUCCESS.equals(r.getCode())) {
            log.warn("merchant audit rejected by merchant-service, merchantId={}, result={}", merchantId, r);
            throw new BizException(CommonCode.SYS_ERROR, "商家服务处理失败");
        }
        return Result.success(null);
    }
}
