package com.yirancrazy.minimall.notify.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDto;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDto;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDto;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDto;
import com.yirancrazy.minimall.notify.service.NotifyService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 standalone MockMvc 验证平台通知分页、广播、营销、系统告警和公告接口的请求解析及服务委托.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PlatformNotifyControllerV1Test {

  private MockMvc mockMvc;
  private NotifyService notifyService;
  private final ObjectMapper mapper = new ObjectMapper();

  /** 创建通知服务 Mock，并构建不启动完整 Spring 容器的平台通知控制器 MockMvc. */
  @BeforeEach
  void setUp() {
    notifyService = mock(NotifyService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PlatformNotifyControllerV1(notifyService)).build();
  }

  /** 验证平台分页服务返回空游标页时，通知接口返回成功业务码00000. */
  @Test
  void testPageReturnsCursorResult() throws Exception {
    when(notifyService.pageVo(any(NotifyListDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(
            get("/api/v1/platform/notify/messages").header("X-User-Id", 1L).param("userId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证合法广播请求携带平台用户身份时返回成功并调用broadcast. */
  @Test
  void testBroadcastWithHeaderInvokesService() throws Exception {
    NotifyBroadcastDto dto = new NotifyBroadcastDto();
    dto.setRecipientType(1);
    dto.setMessageType(1);
    dto.setTitle("公告");
    dto.setContent("内容");
    mockMvc
        .perform(
            post("/api/v1/platform/notify/broadcast")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).broadcast(any(NotifyBroadcastDto.class));
  }

  /** 验证营销请求包含标题正文和两个用户时返回成功并调用marketingPush. */
  @Test
  void testMarketingPushInvokesService() throws Exception {
    NotifyMarketingPushDto dto = new NotifyMarketingPushDto();
    dto.setTitle("促销");
    dto.setContent("满减");
    dto.setUserIds(java.util.List.of(1L, 2L));
    mockMvc
        .perform(
            post("/api/v1/platform/notify/marketing-push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).marketingPush(any(NotifyMarketingPushDto.class));
  }

  /** 验证系统告警请求包含CRITICAL级别时返回成功并调用systemAlert. */
  @Test
  void testSystemAlertInvokesService() throws Exception {
    NotifySystemAlertDto dto = new NotifySystemAlertDto();
    dto.setTitle("告警");
    dto.setContent("服务降级");
    dto.setAlertLevel("CRITICAL");
    mockMvc
        .perform(
            post("/api/v1/platform/notify/system-alert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).systemAlert(any(NotifySystemAlertDto.class));
  }

  /** 验证告警服务返回空游标页时，平台告警接口返回成功业务码00000. */
  @Test
  void testAlertsReturnsCursorResult() throws Exception {
    when(notifyService.alertPageVo(any(SystemAlertPageDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/platform/notify/alerts").header("X-User-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证合法公告请求返回成功并调用publishAnnouncement. */
  @Test
  void testPublishAnnouncementInvokesService() throws Exception {
    AnnouncementCreateDto dto = new AnnouncementCreateDto();
    dto.setTitle("公告标题");
    dto.setContent("公告内容");
    mockMvc
        .perform(
            post("/api/v1/platform/notify/announcements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).publishAnnouncement(any(AnnouncementCreateDto.class));
  }
}
