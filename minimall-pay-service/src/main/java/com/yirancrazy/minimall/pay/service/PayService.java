package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.RefundCreateDTO;
import com.yirancrazy.minimall.pay.vo.RefundVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务接口，定义核心业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
public interface PayService {
    Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount);
    void handleCallback(PayCallbackDTO dto);
    RefundVO createRefund(RefundCreateDTO dto);
}