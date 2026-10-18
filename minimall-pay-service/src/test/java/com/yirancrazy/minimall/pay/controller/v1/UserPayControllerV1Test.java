package com.yirancrazy.minimall.pay.controller.v1;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCreateDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserPayControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class UserPayControllerV1Test {

    private MockMvc mockMvc;
    private PayService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(PayService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserPayControllerV1(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }


    @Test
    void create_returns_payment_id() throws Exception {
        when(service.createPayment(any(), any(), any(), any(), any())).thenReturn(100L);
        PayCreateDTO dto = new PayCreateDTO("NO20260805001", new BigDecimal("10.00"), PayChannelEnum.ALIPAY);
        mockMvc.perform(post("/api/v1/user/pay/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
        verify(service).createPayment(any(), any(), any(), any(), any());
    }


    @Test
    void create_accepts_channel_alias_string() throws Exception {
        when(service.createPayment(any(), any(), any(), any(), any())).thenReturn(100L);
        mockMvc.perform(post("/api/v1/user/pay/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"NO1\",\"amount\":10,\"channel\":\"ALIPAY\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).createPayment(eq("NO1"), eq(1L), isNull(), any(), eq(1));
    }


    @Test
    void create_rejects_unknown_channel() throws Exception {
        mockMvc.perform(post("/api/v1/user/pay/create")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNo\":\"NO1\",\"amount\":10,\"channel\":\"FOO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20001"))
            .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }


    @Test
    void callback_returns_success() throws Exception {
        when(service.handleAlipayCallback(any())).thenReturn(true);
        mockMvc.perform(post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
        verify(service).handleAlipayCallback(any());
    }


    @Test
    void callback_signature_failure_returns_fail() throws Exception {
        when(service.handleAlipayCallback(any())).thenReturn(false);
        mockMvc.perform(post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(content().string("fail"));
        verify(service).handleAlipayCallback(any());
    }


    @Test
    void callback_passes_total_amount_to_service() throws Exception {
        when(service.handleAlipayCallback(any())).thenReturn(true);
        mockMvc.perform(post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS")
                .param("total_amount", "10.00"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
        ArgumentCaptor<AlipayCallbackDTO> cap = ArgumentCaptor.forClass(AlipayCallbackDTO.class);
        verify(service).handleAlipayCallback(cap.capture());
        assertEquals("10.00", cap.getValue().getTotalAmount());
    }


    @Test
    void callback_trade_closed_marks_not_success() throws Exception {
        when(service.handleAlipayCallback(any())).thenReturn(true);
        mockMvc.perform(post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_CLOSED"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
        ArgumentCaptor<AlipayCallbackDTO> cap = ArgumentCaptor.forClass(AlipayCallbackDTO.class);
        verify(service).handleAlipayCallback(cap.capture());
        assertEquals("TRADE_CLOSED", cap.getValue().getTradeStatus());
    }


    @Test
    void createRefund_endpoint_is_removed() throws Exception {
        RefundCreateDTO dto = new RefundCreateDTO(1L, new BigDecimal("10.00"), "damaged");

        mockMvc.perform(post("/api/v1/user/pay/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20000"));

        verifyNoInteractions(service);
    }


    @Test
    void getStatus_returns_vo() throws Exception {
        PayTransactionVO vo = new PayTransactionVO();
        vo.setPaymentNo("P001");
        vo.setOrderNo("NO123");
        vo.setUserId(1L);
        vo.setAmount(new BigDecimal("10.00"));
        vo.setStatus(2);
        vo.setChannel(1);
        when(service.getStatusVO("NO123", 1L)).thenReturn(vo);
        mockMvc.perform(get("/api/v1/user/pay/status/NO123").header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentNo").value("P001"));
    }


    @Test
    void getStatus_without_user_header_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/user/pay/status/NO123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("14003"));

        verifyNoInteractions(service);
    }


    @Test
    void getParams_returns_vo() throws Exception {
        PaymentParamsVO vo = new PaymentParamsVO();
        vo.setPaymentNo("P001");
        when(service.getPaymentParams("P001", 1L)).thenReturn(vo);
        mockMvc.perform(get("/api/v1/user/pay/params/P001").header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentNo").value("P001"));
    }


    @Test
    void getParams_without_user_header_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/user/pay/params/P001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("14003"));

        verifyNoInteractions(service);
    }
}
