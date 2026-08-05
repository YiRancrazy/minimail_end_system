package com.yirancrazy.minimall.notify.controller.v1;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDTO;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformNotifyControllerV1 MockMvc 单元测试，验证平台站内信分页、广播、营销推送与公告端点。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class PlatformNotifyControllerV1Test {

    private MockMvc mockMvc;
    private NotifyService notifyService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        notifyService = mock(NotifyService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlatformNotifyControllerV1(notifyService)).build();
    }

    /**
     * 验证 GET /api/v1/platform/notify/messages 通过 X-User-Id 头返回平台站内信分页。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(notifyService.page(any(NotifyListDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/notify/messages")
                .header("X-User-Id", 1L)
                .param("userId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 POST /api/v1/platform/notify/broadcast 通过 X-User-Id 注入发送者并广播消息。
     */
    @Test
    void broadcast_with_header_invokes_service() throws Exception {
        NotifyBroadcastDTO dto = new NotifyBroadcastDTO();
        dto.setRecipientType(1);
        dto.setMessageType(1);
        dto.setTitle("公告");
        dto.setContent("内容");
        mockMvc.perform(post("/api/v1/platform/notify/broadcast")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).broadcast(any(NotifyBroadcastDTO.class));
    }

    /**
     * 验证 POST /api/v1/platform/notify/marketing-push 调用 service 发送营销推送。
     */
    @Test
    void marketingPush_invokes_service() throws Exception {
        NotifyMarketingPushDTO dto = new NotifyMarketingPushDTO();
        dto.setTitle("促销");
        dto.setContent("满减");
        dto.setUserIds(java.util.List.of(1L, 2L));
        mockMvc.perform(post("/api/v1/platform/notify/marketing-push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).marketingPush(any(NotifyMarketingPushDTO.class));
    }

    /**
     * 验证 POST /api/v1/platform/notify/system-alert 调用 service 发送系统告警。
     */
    @Test
    void systemAlert_invokes_service() throws Exception {
        NotifySystemAlertDTO dto = new NotifySystemAlertDTO();
        dto.setTitle("告警");
        dto.setContent("服务降级");
        dto.setAlertLevel("CRITICAL");
        mockMvc.perform(post("/api/v1/platform/notify/system-alert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).systemAlert(any(NotifySystemAlertDTO.class));
    }

    /**
     * 验证 GET /api/v1/platform/notify/alerts 返回系统告警分页结果。
     */
    @Test
    void alerts_returns_cursor_result() throws Exception {
        when(notifyService.alertPage(any(SystemAlertPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/notify/alerts")
                .header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 POST /api/v1/platform/notify/announcements 发布系统公告。
     */
    @Test
    void publishAnnouncement_invokes_service() throws Exception {
        AnnouncementCreateDTO dto = new AnnouncementCreateDTO();
        dto.setTitle("公告标题");
        dto.setContent("公告内容");
        mockMvc.perform(post("/api/v1/platform/notify/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(notifyService).publishAnnouncement(any(AnnouncementCreateDTO.class));
    }
}
