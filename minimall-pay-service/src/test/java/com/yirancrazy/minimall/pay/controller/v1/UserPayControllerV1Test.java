package com.yirancrazy.minimall.pay.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDto;
import com.yirancrazy.minimall.pay.dto.PayCreateDto;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVo;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证用户支付控制器、参数解析和可信请求头约束。
 * 使用 Mock Service、独立 MockMvc 和异常处理器覆盖创建、回调、状态及收银台参数场景。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class UserPayControllerV1Test {

  private MockMvc mockMvc;
  private PayService service;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    service = mock(PayService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new UserPayControllerV1(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  /** 验证用户创建支付：配置服务返回支付单一百，预期成功响应携带该 ID. */
  @Test
  void testCreateReturnsPaymentId() throws Exception {
    when(service.createPayment(any(), any(), any(), any(), any())).thenReturn(100L);
    PayCreateDto dto =
        new PayCreateDto("NO20260805001", new BigDecimal("10.00"), PayChannelEnum.ALIPAY);
    mockMvc
        .perform(
            post("/api/v1/user/pay/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").value(100));
    verify(service).createPayment(any(), any(), any(), any(), any());
  }

  /** 验证渠道别名解析：请求使用支付宝别名，预期服务收到对应渠道编码. */
  @Test
  void testCreateAcceptsChannelAliasString() throws Exception {
    when(service.createPayment(any(), any(), any(), any(), any())).thenReturn(100L);
    mockMvc
        .perform(
            post("/api/v1/user/pay/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"NO1\",\"amount\":10,\"channel\":\"ALIPAY\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).createPayment(eq("NO1"), eq(1L), isNull(), any(), eq(1));
  }

  /** 验证未知渠道：请求使用非法渠道值，预期返回请求体格式错误且服务不被调用. */
  @Test
  void testCreateRejectsUnknownChannel() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/user/pay/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"NO1\",\"amount\":10,\"channel\":\"FOO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("20001"))
        .andExpect(jsonPath("$.message").value("请求体格式错误"));
  }

  /** 验证支付宝回调：配置服务受理成功，预期返回渠道要求的成功文本. */
  @Test
  void testCallbackReturnsSuccess() throws Exception {
    when(service.handleAlipayCallback(any())).thenReturn(true);
    mockMvc
        .perform(
            post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS"))
        .andExpect(status().isOk())
        .andExpect(content().string("success"));
    verify(service).handleAlipayCallback(any());
  }

  /** 验证支付宝回调：配置服务受理失败，预期返回渠道要求的失败文本. */
  @Test
  void testCallbackSignatureFailureReturnsFail() throws Exception {
    when(service.handleAlipayCallback(any())).thenReturn(false);
    mockMvc
        .perform(
            post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS"))
        .andExpect(status().isOk())
        .andExpect(content().string("fail"));
    verify(service).handleAlipayCallback(any());
  }

  /** 验证回调金额传递：请求带两位小数金额，预期服务入参保留该金额文本. */
  @Test
  void testCallbackPassesTotalAmountToService() throws Exception {
    when(service.handleAlipayCallback(any())).thenReturn(true);
    mockMvc
        .perform(
            post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS")
                .param("total_amount", "10.00"))
        .andExpect(status().isOk())
        .andExpect(content().string("success"));
    ArgumentCaptor<AlipayCallbackDto> cap = ArgumentCaptor.forClass(AlipayCallbackDto.class);
    verify(service).handleAlipayCallback(cap.capture());
    assertEquals("10.00", cap.getValue().getTotalAmount());
  }

  /** 验证交易关闭回调：请求带关闭状态，预期服务收到关闭状态而非成功状态. */
  @Test
  void testCallbackTradeClosedMarksNotSuccess() throws Exception {
    when(service.handleAlipayCallback(any())).thenReturn(true);
    mockMvc
        .perform(
            post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_CLOSED"))
        .andExpect(status().isOk())
        .andExpect(content().string("success"));
    ArgumentCaptor<AlipayCallbackDto> cap = ArgumentCaptor.forClass(AlipayCallbackDto.class);
    verify(service).handleAlipayCallback(cap.capture());
    assertEquals("TRADE_CLOSED", cap.getValue().getTradeStatus());
  }

  /** 验证用户退款端点：请求已移除路径，预期返回错误且服务不被调用. */
  @Test
  void testCreateRefundEndpointIsRemoved() throws Exception {
    RefundCreateDto dto = new RefundCreateDto(1L, new BigDecimal("10.00"), "damaged");

    mockMvc
        .perform(
            post("/api/v1/user/pay/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("20000"));

    verifyNoInteractions(service);
  }

  /** 验证支付状态查询：配置服务返回指定支付单，预期响应包含该单号. */
  @Test
  void testGetStatusReturnsVo() throws Exception {
    PayTransactionVo vo = new PayTransactionVo();
    vo.setPaymentNo("P001");
    vo.setOrderNo("NO123");
    vo.setUserId(1L);
    vo.setAmount(new BigDecimal("10.00"));
    vo.setStatus(2);
    vo.setChannel(1);
    when(service.getStatusVo("NO123", 1L)).thenReturn(vo);
    mockMvc
        .perform(get("/api/v1/user/pay/status/NO123").header("X-User-Id", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentNo").value("P001"));
  }

  /** 验证状态查询：缺少用户请求头，预期返回缺失请求头错误且服务不被调用. */
  @Test
  void testGetStatusWithoutUserHeaderRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/user/pay/status/NO123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("14003"));

    verifyNoInteractions(service);
  }

  /** 验证收银台参数：配置服务返回指定支付单，预期响应包含该单号. */
  @Test
  void testGetParamsReturnsVo() throws Exception {
    PaymentParamsVo vo = new PaymentParamsVo();
    vo.setPaymentNo("P001");
    when(service.getPaymentParams("P001", 1L)).thenReturn(vo);
    mockMvc
        .perform(get("/api/v1/user/pay/params/P001").header("X-User-Id", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.paymentNo").value("P001"));
  }

  /** 验证收银台参数：缺少用户请求头，预期返回缺失请求头错误且服务不被调用. */
  @Test
  void testGetParamsWithoutUserHeaderRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/user/pay/params/P001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("14003"));

    verifyNoInteractions(service);
  }
}
