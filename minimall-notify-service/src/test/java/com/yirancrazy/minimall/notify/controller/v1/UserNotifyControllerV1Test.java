package com.yirancrazy.minimall.notify.controller.v1;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyBatchDeleteDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserNotifyControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class UserNotifyControllerV1Test {

    private MockMvc mockMvc;
    private NotifyService notifyService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        notifyService = mock(NotifyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserNotifyControllerV1(notifyService)).build();
    }


    @Test
    void page_returns_cursor_result() throws Exception {
        when(notifyService.pageVO(any(NotifyListDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/user/notify/messages")
                .header("X-User-Id", 1L)
                .param("userId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }


    @Test
    void unreadCount_returns_long() throws Exception {
        when(notifyService.unreadCount(anyInt(), anyLong())).thenReturn(5L);
        mockMvc.perform(get("/api/v1/user/notify/messages/_count")
                .header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(5));
    }


    @Test
    void markRead_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/notify/messages/99/_read")
                .header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).markRead(anyLong(), anyInt(), anyLong());
    }


    @Test
    void markAllRead_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/notify/messages/_read-all")
                .header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).markAllRead(anyInt(), anyLong());
    }


    @Test
    void batchDelete_invokes_service() throws Exception {
        NotifyBatchDeleteDTO dto = new NotifyBatchDeleteDTO();
        dto.setIds(java.util.List.of(1L, 2L, 3L));
        mockMvc.perform(delete("/api/v1/user/notify/messages/batch")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).batchDelete(any(), anyInt(), anyLong());
    }


    @Test
    void list_overrides_userId_from_header() throws Exception {
        NotifyMessageVO vo = new NotifyMessageVO();
        vo.setId(1L);
        vo.setUserId(1L);
        vo.setTitle("通知");
        vo.setContent("内容");
        when(notifyService.listByUserVO(any(NotifyListDTO.class)))
            .thenReturn(java.util.List.of(vo));
        mockMvc.perform(get("/api/v1/user/notify")
                .header("X-User-Id", 1L)
                .param("userId", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data[0].id").value(1));
        ArgumentCaptor<NotifyListDTO> captor = ArgumentCaptor.forClass(NotifyListDTO.class);
        verify(notifyService).listByUserVO(captor.capture());
        NotifyListDTO captured = captor.getValue();
        assertEquals(1L, captured.getUserId());
        assertEquals(RecipientTypeEnum.USER.intCode(), captured.getRecipientType());
    }
}
