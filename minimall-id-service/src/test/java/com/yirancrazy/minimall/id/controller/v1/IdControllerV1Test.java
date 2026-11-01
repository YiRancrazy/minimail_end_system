package com.yirancrazy.minimall.id.controller.v1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.id.service.IdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部 ID 接口的 standalone MockMvc 测试。使用 Mockito 替身替换 IdService，
 *     在不启动 Spring 上下文的情况下验证 HTTP 状态、响应封装、数值载荷和委托调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class IdControllerV1Test {

    private MockMvc mockMvc;
    private IdService idService;

    /**
     * 每个测试前创建 IdService 替身和 standalone MockMvc。
     */
    @BeforeEach
    void setUp() {
        idService = mock(IdService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IdControllerV1(idService)).build();
    }

    /** 验证内部发号接口返回长整型ID：IdService对ORDER返回42，GET请求后预期200、成功码00000且data为42. */
    @Test
    void testNextReturnsLongId() throws Exception {
        when(idService.nextId("ORDER")).thenReturn(42L);
        mockMvc
                .perform(get("/internal/id/next").param("bizTag", "ORDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").isNumber())
                .andExpect(jsonPath("$.data").value(42));
        verify(idService).nextId("ORDER");
    }
}
