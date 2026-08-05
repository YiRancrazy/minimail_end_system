package com.yirancrazy.minimall.notify.controller.v1;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantNotifyControllerV1 MockMvc 单元测试，验证商家站内信分页、未读计数、标记已读与删除端点。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class MerchantNotifyControllerV1Test {

    private MockMvc mockMvc;
    private NotifyService notifyService;

    @BeforeEach
    void setUp() {
        notifyService = mock(NotifyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantNotifyControllerV1(notifyService)).build();
    }

    /**
     * 验证 GET /api/v1/merchant/notify/messages 通过 X-Merchant-Id 头返回游标分页结果。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(notifyService.page(any(NotifyListDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/notify/messages")
                .header("X-Merchant-Id", 2L)
                .param("userId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 GET /api/v1/merchant/notify/messages/_count 返回未读消息数。
     */
    @Test
    void unreadCount_returns_long() throws Exception {
        when(notifyService.unreadCount(anyInt(), anyLong())).thenReturn(3L);
        mockMvc.perform(get("/api/v1/merchant/notify/messages/_count")
                .header("X-Merchant-Id", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(3));
    }

    /**
     * 验证 POST /api/v1/merchant/notify/messages/{id}/_read 调用 service 标记单条已读。
     */
    @Test
    void markRead_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/notify/messages/99/_read")
                .header("X-Merchant-Id", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).markRead(anyLong(), anyInt(), anyLong());
    }

    /**
     * 验证 POST /api/v1/merchant/notify/messages/_read-all 调用 service 全部标记已读。
     */
    @Test
    void markAllRead_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/notify/messages/_read-all")
                .header("X-Merchant-Id", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).markAllRead(anyInt(), anyLong());
    }

    /**
     * 验证 DELETE /api/v1/merchant/notify/messages/{id} 调用 service 软删除单条消息。
     */
    @Test
    void delete_invokes_service() throws Exception {
        mockMvc.perform(delete("/api/v1/merchant/notify/messages/99")
                .header("X-Merchant-Id", 2L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).delete(anyLong(), anyInt(), anyLong());
    }
}
