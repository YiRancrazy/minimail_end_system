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
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家资质审核控制器。merchant-service 通过 MQ/Feign 提供数据；
 *              本端点提供契约占位，前端 MSW 在 dev 环境兜底。
 * @Version: 1.0
 * @DateTime: 2026/08/09
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/merchants")
public class PlatformMerchantQualificationControllerV1 {

    /**
     * 平台分页查询商家资质列表（仅返回当前 PENDING 状态）。
     * 数据由 merchant-service 提供；本端作为契约入口。
     * @param cursor 游标
     * @param limit  每页条数
     */
    @GetMapping("/qualifications")
    public Result<CursorPageVO<Object>> listPending(@RequestParam(required = false) String cursor,
                                                     @RequestParam(defaultValue = "20") int limit) {
        CursorPageVO<Object> page = new CursorPageVO<>(List.of(), null, false, limit);
        log.info("list pending merchant qualifications, cursor={}, limit={}", cursor, limit);
        return Result.success(page);
    }

    /**
     * 平台审核商家资质。
     * @param merchantId 商家ID（路径占位，与后端归属校验对齐）
     * @param dto 审核入参
     */
    @PostMapping("/{merchantId}/qualifications/audit")
    public Result<Void> audit(@PathVariable Long merchantId,
                               @Valid @RequestBody MerchantQualificationAuditDTO dto) {
        log.info("platform audit merchant qualification, merchantId={}, qualificationId={}, approved={}, reason={}",
                merchantId, dto.getQualificationId(), dto.getApproved(), dto.getReason());
        return Result.success(null);
    }
}