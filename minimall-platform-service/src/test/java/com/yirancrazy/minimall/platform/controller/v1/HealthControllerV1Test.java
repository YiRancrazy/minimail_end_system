package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: HealthControllerV1 单元测试，验证健康检查返回 `platform-ok`。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HealthControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    /**
     * 通过随机端口启动应用上下文，调用 `/internal/platform/health`，断言 HTTP 状态为 200，
     * 且响应体 Result 中 code 为成功码 00000、data 为 `platform-ok`。
     */
    @Test
    public void health_returns_platform_ok() {
        ResponseEntity<Result> r = rest.getForEntity(
            "http://localhost:" + port + "/internal/platform/health", Result.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("00000", r.getBody().getCode());
        assertEquals("platform-ok", r.getBody().getData());
    }
}