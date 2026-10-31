package com.yirancrazy.minimall.notify.controller.v1;

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
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyBatchDeleteDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVo;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 standalone MockMvc 验证用户通知分页、未读计数、已读、批量删除以及请求头用户编号覆盖逻辑.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class UserNotifyControllerV1Test {

  private MockMvc mockMvc;
  private NotifyService notifyService;
  private final ObjectMapper mapper = new ObjectMapper();

  /** 创建通知服务 Mock，并构建不启动完整 Spring 容器的用户通知控制器 MockMvc. */
  @BeforeEach
  void setUp() {
    notifyService = mock(NotifyService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new UserNotifyControllerV1(notifyService)).build();
  }

  /** 验证用户分页服务返回空游标页时，通知接口返回成功业务码00000. */
  @Test
  void testPageReturnsCursorResult() throws Exception {
    when(notifyService.pageVo(any(NotifyListDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/user/notify/messages").header("X-User-Id", 1L).param("userId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证未读计数服务返回5时，用户接口返回成功业务码和未读数5. */
  @Test
  void testUnreadCountReturnsLong() throws Exception {
    when(notifyService.unreadCount(anyInt(), anyLong())).thenReturn(5L);
    mockMvc
        .perform(get("/api/v1/user/notify/messages/_count").header("X-User-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").value(5));
  }

  /** 验证用户已读请求携带通知编号和用户身份时成功并调用markRead. */
  @Test
  void testMarkReadInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/user/notify/messages/99/_read").header("X-User-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).markRead(anyLong(), anyInt(), anyLong());
  }

  /** 验证用户全部已读请求携带用户身份时成功并调用markAllRead. */
  @Test
  void testMarkAllReadInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/user/notify/messages/_read-all").header("X-User-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).markAllRead(anyInt(), anyLong());
  }

  /** 验证批量删除请求包含三个消息编号时成功并调用batchDelete. */
  @Test
  void testBatchDeleteInvokesService() throws Exception {
    NotifyBatchDeleteDto dto = new NotifyBatchDeleteDto();
    dto.setIds(java.util.List.of(1L, 2L, 3L));
    mockMvc
        .perform(
            delete("/api/v1/user/notify/messages/batch")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).batchDelete(any(), anyInt(), anyLong());
  }

  /** 验证查询参数伪造用户编号时，列表仍使用请求头用户并返回对应消息. */
  @Test
  void testListOverridesUserIdFromHeader() throws Exception {
    NotifyMessageVo vo = new NotifyMessageVo();
    vo.setId(1L);
    vo.setUserId(1L);
    vo.setTitle("通知");
    vo.setContent("内容");
    when(notifyService.listByUserVo(any(NotifyListDto.class))).thenReturn(java.util.List.of(vo));
    mockMvc
        .perform(get("/api/v1/user/notify").header("X-User-Id", 1L).param("userId", "999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data[0].id").value(1));
    ArgumentCaptor<NotifyListDto> captor = ArgumentCaptor.forClass(NotifyListDto.class);
    verify(notifyService).listByUserVo(captor.capture());
    NotifyListDto captured = captor.getValue();
    assertEquals(1L, captured.getUserId());
    assertEquals(RecipientTypeEnum.USER.intCode(), captured.getRecipientType());
  }
}
