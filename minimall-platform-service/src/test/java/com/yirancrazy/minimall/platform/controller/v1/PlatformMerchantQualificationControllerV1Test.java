package com.yirancrazy.minimall.platform.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantQualificationControllerV1 单元测试，验证审核委托 MerchantFeignClient 真实落库，
 *              远端失败时抛异常而非静默成功。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@ExtendWith(MockitoExtension.class)
class PlatformMerchantQualificationControllerV1Test {

    @Mock private MerchantFeignClient merchantFeignClient;

    private PlatformMerchantQualificationControllerV1 controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformMerchantQualificationControllerV1(merchantFeignClient);
    }

    @Test
    void audit_approved_delegates_to_merchant_service() {
        when(merchantFeignClient.audit(1L, true, null)).thenReturn(Result.success(null));

        MerchantQualificationAuditDTO dto = new MerchantQualificationAuditDTO();
        dto.setQualificationId(100L);
        dto.setApproved(true);

        Result<Void> result = controller.audit(1L, dto);

        assertEquals("00000", result.getCode());
        verify(merchantFeignClient).audit(1L, true, null);
    }

    @Test
    void audit_rejected_passes_reason_to_merchant_service() {
        when(merchantFeignClient.audit(1L, false, "证照不清晰")).thenReturn(Result.success(null));

        MerchantQualificationAuditDTO dto = new MerchantQualificationAuditDTO();
        dto.setQualificationId(100L);
        dto.setApproved(false);
        dto.setReason("证照不清晰");

        Result<Void> result = controller.audit(1L, dto);

        assertEquals("00000", result.getCode());
        verify(merchantFeignClient).audit(1L, false, "证照不清晰");
    }

    @Test
    void audit_throws_when_merchant_service_rejects() {
        when(merchantFeignClient.audit(1L, true, null))
                .thenReturn(Result.fail(CommonCode.SYS_ERROR, "商家服务不可用"));

        MerchantQualificationAuditDTO dto = new MerchantQualificationAuditDTO();
        dto.setQualificationId(100L);
        dto.setApproved(true);

        assertThrows(BizException.class, () -> controller.audit(1L, dto));
    }
}
