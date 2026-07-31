package com.yirancrazy.minimall.auth.controller.v1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: HealthControllerV1 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class HealthControllerV1Test {

    @Test
    void health_returns_auth_ok() {
        HealthControllerV1 controller = new HealthControllerV1();
        var result = controller.health();
        assertEquals("00000", result.getCode());
        assertEquals("auth-ok", result.getData());
    }
}
