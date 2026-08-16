package com.yirancrazy.minimall.id.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.id.service.IdService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdControllerV1 MockMvc 单元测试，验证 /internal/id/next 的路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
class IdControllerV1Test {

    private MockMvc mockMvc;
    private IdService idService;

    @BeforeEach
    void setUp() {
        idService = mock(IdService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IdControllerV1(idService)).build();
    }

    /**
     * 验证 GET /internal/id/next 按 bizTag 返回 Long 型 ID。
     */
    @Test
    void next_returns_long_id() throws Exception {
        when(idService.nextId("ORDER")).thenReturn(42L);
        mockMvc.perform(get("/internal/id/next").param("bizTag", "ORDER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").isNumber())
            .andExpect(jsonPath("$.data").value(42));
        verify(idService).nextId("ORDER");
    }
}
