package com.yirancrazy.minimall.pay.controller.v1;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.yirancrazy.minimall.pay.dto.PayCreateDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserPayControllerV1 MockMvc 单元测试，验证用户端支付 HTTP 路由、支付创建、回调与状态查询。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class UserPayControllerV1Test {

    private MockMvc mockMvc;
    private PayService service;
    private PayGateway gateway;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(PayService.class);
        gateway = mock(PayGateway.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserPayControllerV1(service, gateway))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    /**
     * 验证 POST /api/v1/user/pay/create 调用 service 并返回支付流水ID。
     */
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

    /**
     * 验证 channel 支持字符串枚举名（ALIPAY）反序列化，并转换为数字 code 传给 service。
     */
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

    /**
     * 验证非法 channel 反序列化失败时返回友好参数错误而非系统繁忙。
     */
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

    /**
     * 验证 POST /api/v1/user/pay/callback/alipay 验签通过后处理 TRADE_SUCCESS 回调返回 success。
     */
    @Test
    void callback_returns_success() throws Exception {
        when(gateway.verifyCallback(any())).thenReturn("T123");
        mockMvc.perform(post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(content().string("success"));
        verify(service).handleCallback(any());
    }

    /**
     * 验证回调验签失败时返回 fail 且不推进业务状态。
     */
    @Test
    void callback_signature_failure_returns_fail() throws Exception {
        when(gateway.verifyCallback(any()))
            .thenThrow(new RuntimeException("signature verification failed"));
        mockMvc.perform(post("/api/v1/user/pay/callback/alipay")
                .param("trade_no", "T123")
                .param("out_trade_no", "P456")
                .param("trade_status", "TRADE_SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(content().string("fail"));
        verify(service, never()).handleCallback(any());
    }

    /**
     * 验证 POST /api/v1/user/pay/refunds 调用 service 并返回退款 VO。
     */
    @Test
    void createRefund_returns_vo() throws Exception {
        RefundVO vo = new RefundVO("R001", "P456", new BigDecimal("10.00"), 2);
        when(service.createRefund(any())).thenReturn(vo);
        RefundCreateDTO dto = new RefundCreateDTO(1L, new BigDecimal("10.00"), "damaged");
        mockMvc.perform(post("/api/v1/user/pay/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundNo").value("R001"));
        verify(service).createRefund(any());
    }

    /**
     * 验证 GET /api/v1/user/pay/status/{orderNo} 返回支付流水 VO。
     */
    @Test
    void getStatus_returns_vo() throws Exception {
        PayTransactionPO po = new PayTransactionPO();
        po.setPaymentNo("P001");
        po.setOrderNo("NO123");
        po.setUserId(1L);
        po.setAmount(new BigDecimal("10.00"));
        po.setStatus(2);
        po.setChannel(1);
        when(service.getByOrderNo("NO123")).thenReturn(po);
        mockMvc.perform(get("/api/v1/user/pay/status/NO123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentNo").value("P001"));
    }

    /**
     * 验证 GET /api/v1/user/pay/params/{paymentNo} 返回支付参数 VO。
     */
    @Test
    void getParams_returns_vo() throws Exception {
        PaymentParamsVO vo = new PaymentParamsVO();
        vo.setPaymentNo("P001");
        when(service.getPaymentParams("P001")).thenReturn(vo);
        mockMvc.perform(get("/api/v1/user/pay/params/P001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentNo").value("P001"));
    }
}
