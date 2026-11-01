package com.yirancrazy.minimall.platform.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDto;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质审核控制器向商家服务委派审核请求的单元测试.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class PlatformMerchantQualificationControllerV1Test {

    @Mock private PlatformMerchantService platformMerchantService;

    private PlatformMerchantQualificationControllerV1 controller;

    /** 使用 Mockito 商家服务替身为每个测试创建控制器实例。 */
    @BeforeEach
    void setUp() {
        controller = new PlatformMerchantQualificationControllerV1(platformMerchantService);
    }

    /** 目标：验证审核通过委派；前置：服务替身和资质 100、approved=true；步骤：审核商家 1；预期：code=00000 且传递同一请求. */
    @Test
    void testAuditApprovedDelegatesToMerchantService() {
        MerchantQualificationAuditDto dto = new MerchantQualificationAuditDto();
        dto.setQualificationId(100L);
        dto.setApproved(true);

        Result<Void> result = controller.audit(1L, dto);

        assertEquals("00000", result.getCode());
        verify(platformMerchantService).auditQualification(1L, dto);
    }

    /** 目标：验证拒绝原因传递；前置：服务替身和 approved=false、拒绝原因；步骤：审核商家 1；预期：code=00000 且原因随请求传递. */
    @Test
    void testAuditRejectedPassesReasonToMerchantService() {
        MerchantQualificationAuditDto dto = new MerchantQualificationAuditDto();
        dto.setQualificationId(100L);
        dto.setApproved(false);
        dto.setReason("证照不清晰");

        Result<Void> result = controller.audit(1L, dto);

        assertEquals("00000", result.getCode());
        verify(platformMerchantService).auditQualification(1L, dto);
    }
}
