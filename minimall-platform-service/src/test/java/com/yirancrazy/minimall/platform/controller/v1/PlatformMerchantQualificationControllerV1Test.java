package com.yirancrazy.minimall.platform.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantQualificationControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@ExtendWith(MockitoExtension.class)
class PlatformMerchantQualificationControllerV1Test {

    @Mock private PlatformMerchantService platformMerchantService;

    private PlatformMerchantQualificationControllerV1 controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformMerchantQualificationControllerV1(platformMerchantService);
    }

    @Test
    void audit_approved_delegates_to_merchant_service() {
        MerchantQualificationAuditDTO dto = new MerchantQualificationAuditDTO();
        dto.setQualificationId(100L);
        dto.setApproved(true);

        Result<Void> result = controller.audit(1L, dto);

        assertEquals("00000", result.getCode());
        verify(platformMerchantService).auditQualification(1L, dto);
    }

    @Test
    void audit_rejected_passes_reason_to_merchant_service() {
        MerchantQualificationAuditDTO dto = new MerchantQualificationAuditDTO();
        dto.setQualificationId(100L);
        dto.setApproved(false);
        dto.setReason("证照不清晰");

        Result<Void> result = controller.audit(1L, dto);

        assertEquals("00000", result.getCode());
        verify(platformMerchantService).auditQualification(1L, dto);
    }
}
