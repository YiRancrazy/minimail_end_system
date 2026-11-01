package com.yirancrazy.minimall.merchant.controller.v1;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantControllerV1 HTTP 契约测试；使用 MerchantService mock 和 standalone MockMvc 验证平台角色、驳回原因校验及审核服务委派，不启动 Spring 容器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MerchantControllerV1Test {

    private MockMvc mockMvc;
    private MerchantService merchantService;

    /**
     * 初始化商家服务 mock、控制器和 MockMvc。
     *
     * 注册 GlobalExceptionHandler，使业务异常按统一 Result 结构返回。
     */
    @BeforeEach
    void setUp() {
        merchantService = mock(MerchantService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new MerchantControllerV1(merchantService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    /** 验证平台角色可以通过审核；前置角色 PLATFORM、approved=true；执行审核请求；预期 00000 且服务收到 (99, true, null) 委派. */
    @Test
    void testAuditPlatformRoleSucceeds() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/merchant/merchants/99/audit")
                                .param("approved", "true")
                                .header("X-User-Role", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(merchantService).audit(99L, true, null);
    }

    /** 验证非平台角色不能审核；前置角色 MERCHANT；执行驳回请求；预期返回 20003 且审核服务不被调用. */
    @Test
    void testAuditRejectsMerchantRole() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/merchant/merchants/99/audit")
                                .param("approved", "false")
                                .header("X-User-Role", "MERCHANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20003"));
        verify(merchantService, never()).audit(anyLong(), anyBoolean(), any());
    }

    /** 验证驳回必须提供原因；前置平台角色且未传 reason；执行驳回请求；预期返回 20001 且审核服务不被调用. */
    @Test
    void testAuditRejectMissingReasonReturnsParamInvalid() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/merchant/merchants/99/audit")
                                .param("approved", "false")
                                .header("X-User-Role", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20001"));
        verify(merchantService, never()).audit(anyLong(), anyBoolean(), any());
    }

    /** 验证平台可携带原因驳回；前置 role=PLATFORM、approved=false；执行审核请求；预期 00000 且服务收到相同参数. */
    @Test
    void testAuditRejectWithReasonSucceeds() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/merchant/merchants/99/audit")
                                .param("approved", "false")
                                .param("reason", "资质材料不全")
                                .header("X-User-Role", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(merchantService).audit(99L, false, "资质材料不全");
    }
}
