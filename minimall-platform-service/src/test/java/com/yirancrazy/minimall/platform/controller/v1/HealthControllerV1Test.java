package com.yirancrazy.minimall.platform.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: HealthControllerV1 MockMvc 单元测试，验证健康检查端点的 HTTP 路由与 Result 包装。
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
class HealthControllerV1Test {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthControllerV1()).build();
    }

    /**
     * 验证 GET /api/v1/platform/health 返回 platform-ok 健康状态。
     */
    @Test
    void health_returns_platform_ok() throws Exception {
        mockMvc.perform(get("/api/v1/platform/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value("platform-ok"));
    }

    /**
     * 验证 POST /api/v1/platform/health 返回 405 方法不允许，确认端点仅接受 GET。
     */
    @Test
    void health_rejects_post_method() throws Exception {
        mockMvc.perform(post("/api/v1/platform/health"))
            .andExpect(status().isMethodNotAllowed());
    }
}
