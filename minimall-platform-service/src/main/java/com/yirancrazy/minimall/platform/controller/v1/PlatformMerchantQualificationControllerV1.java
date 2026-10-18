package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantQualificationControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/merchants")
public class PlatformMerchantQualificationControllerV1 {

    private final PlatformMerchantService platformMerchantService;

    /**
     * 构造 PlatformMerchantQualificationControllerV1 实例。
     * @param platformMerchantService platformMerchantService 参数
     */
    public PlatformMerchantQualificationControllerV1(PlatformMerchantService platformMerchantService) {
        this.platformMerchantService = platformMerchantService;
    }


    /**
     * 查询列表数据。
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 统一响应结果
     */
    @GetMapping("/qualifications")
    public Result<CursorPageVO<MerchantQualificationVO>> listPending(@RequestParam(required = false) String cursor,
                                                                    @RequestParam(defaultValue = "20") int limit) {
        return Result.success(platformMerchantService.listPendingQualifications(cursor, limit));
    }


    /**
     * 执行 audit 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{merchantId}/qualifications/audit")
    public Result<Void> audit(@PathVariable Long merchantId,
                               @Valid @RequestBody MerchantQualificationAuditDTO dto) {
        platformMerchantService.auditQualification(merchantId, dto);
        return Result.success(null);
    }
}
