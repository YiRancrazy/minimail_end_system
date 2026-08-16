package com.yirancrazy.minimall.merchant.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.merchant.service.MerchantService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantControllerV1 MockMvc 单元测试，验证资质审核端点的角色权限控制与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
class MerchantControllerV1Test {

    private MockMvc mockMvc;
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        merchantService = mock(MerchantService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantControllerV1(merchantService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    /**
     * 验证 PLATFORM 角色 POST /{id}/audit 审核通过时返回成功并进入 service。
     */
    @Test
    void audit_platform_role_succeeds() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/merchants/99/audit")
                .param("approved", "true")
                .header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(merchantService).audit(99L, true, null);
    }

    /**
     * 验证 MERCHANT 角色调用审核端点时返回 FORBIDDEN(20003) 且不进入 service。
     */
    @Test
    void audit_rejects_merchant_role() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/merchants/99/audit")
                .param("approved", "false")
                .header("X-User-Role", "MERCHANT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(merchantService, never()).audit(anyLong(), anyBoolean(), any());
    }

    /**
     * 验证 PLATFORM 角色驳回但不填原因时返回 PARAM_INVALID(20001) 且不进入 service。
     */
    @Test
    void audit_reject_missing_reason_returns_param_invalid() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/merchants/99/audit")
                .param("approved", "false")
                .header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20001"));
        verify(merchantService, never()).audit(anyLong(), anyBoolean(), any());
    }

    /**
     * 验证 PLATFORM 角色驳回且填写原因时进入 service。
     */
    @Test
    void audit_reject_with_reason_succeeds() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/merchants/99/audit")
                .param("approved", "false")
                .param("reason", "资质材料不全")
                .header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(merchantService).audit(99L, false, "资质材料不全");
    }
}
