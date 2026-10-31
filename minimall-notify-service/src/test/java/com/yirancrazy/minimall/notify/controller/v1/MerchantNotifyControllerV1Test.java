package com.yirancrazy.minimall.notify.controller.v1;

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

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.service.NotifyService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 standalone MockMvc 验证商家通知控制器的请求头身份传递、游标响应及状态操作对 NotifyService 的委托.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MerchantNotifyControllerV1Test {

  private MockMvc mockMvc;
  private NotifyService notifyService;

  /** 创建通知服务 Mock，并构建不启动完整 Spring 容器的商家通知控制器 MockMvc. */
  @BeforeEach
  void setUp() {
    notifyService = mock(NotifyService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new MerchantNotifyControllerV1(notifyService)).build();
  }

  /** 验证分页服务返回空游标页时，商家通知接口返回成功业务码00000. */
  @Test
  void testPageReturnsCursorResult() throws Exception {
    when(notifyService.pageVo(any(NotifyListDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(
            get("/api/v1/merchant/notify/messages")
                .header("X-Merchant-Id", 2L)
                .param("userId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证未读计数服务返回3时，商家接口返回成功业务码和未读数3. */
  @Test
  void testUnreadCountReturnsLong() throws Exception {
    when(notifyService.unreadCount(anyInt(), anyLong())).thenReturn(3L);
    mockMvc
        .perform(get("/api/v1/merchant/notify/messages/_count").header("X-Merchant-Id", 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").value(3));
  }

  /** 验证商家已读请求携带通知编号和商家身份时成功并调用markRead. */
  @Test
  void testMarkReadInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/merchant/notify/messages/99/_read").header("X-Merchant-Id", 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).markRead(anyLong(), anyInt(), anyLong());
  }

  /** 验证商家全部已读请求携带商家身份时成功并调用markAllRead. */
  @Test
  void testMarkAllReadInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/merchant/notify/messages/_read-all").header("X-Merchant-Id", 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).markAllRead(anyInt(), anyLong());
  }

  /** 验证商家删除请求携带通知编号和商家身份时成功并调用delete. */
  @Test
  void testDeleteInvokesService() throws Exception {
    mockMvc
        .perform(delete("/api/v1/merchant/notify/messages/99").header("X-Merchant-Id", 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(notifyService).delete(anyLong(), anyInt(), anyLong());
  }
}
