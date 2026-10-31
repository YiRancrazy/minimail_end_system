package com.yirancrazy.minimall.auth.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证服务健康接口单元测试；验证固定成功码和存活标识，不启动 Spring 容器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class HealthControllerV1Test {

  /** 验证健康检查成功：直接创建控制器，调用 health 后预期成功码为 00000 且数据为 auth-ok. */
  @Test
  void testHealthReturnsAuthOk() {
    HealthControllerV1 controller = new HealthControllerV1();
    var result = controller.health();
    assertEquals("00000", result.getCode());
    assertEquals("auth-ok", result.getData());
  }
}
