package com.yirancrazy.minimall.platform.controller.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台健康检查接口的 HTTP 方法与成功响应测试.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class HealthControllerV1Test {

  private MockMvc mockMvc;

  /** 为每个测试创建仅注册 HealthControllerV1 的独立 MockMvc。 */
  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new HealthControllerV1()).build();
  }

  /** 目标：验证健康检查成功响应；前置：MockMvc 注册控制器；步骤：GET /api/v1/platform/health；预期：HTTP 200、code=00000、data=platform-ok. */
  @Test
  void testHealthReturnsPlatformOk() throws Exception {
    mockMvc
        .perform(get("/api/v1/platform/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").value("platform-ok"));
  }

  /** 目标：验证健康检查拒绝 POST；前置：MockMvc 注册控制器；步骤：POST /api/v1/platform/health；预期：HTTP 405，不执行健康响应. */
  @Test
  void testHealthRejectsPostMethod() throws Exception {
    mockMvc.perform(post("/api/v1/platform/health")).andExpect(status().isMethodNotAllowed());
  }
}
