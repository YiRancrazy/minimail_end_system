package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.RefundCreateDTO;
import com.yirancrazy.minimall.pay.vo.RefundVO;

public interface PayService {
    Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount);
    void handleCallback(PayCallbackDTO dto);
    RefundVO createRefund(RefundCreateDTO dto);
}