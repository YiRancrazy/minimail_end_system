package com.yirancrazy.minimall.platform.controller.v1;

import java.util.List;
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
     * 平台分页查询商家资质列表（仅返回当前 PENDING 状态）。
     * 数据由 merchant-service 提供；本端作为契约入口。
     * @param cursor 游标
     * @param limit  每页条数
     */
    @GetMapping("/qualifications")
    public Result<CursorPageVO<MerchantQualificationVO>> listPending(@RequestParam(required = false) String cursor,
                                                                    @RequestParam(defaultValue = "20") int limit) {
        CursorPageVO<MerchantQualificationVO> page =
                new CursorPageVO<>(List.of(), null, false, limit);
        log.info("list pending merchant qualifications, cursor={}, limit={}", cursor, limit);
        return Result.success(page);
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
