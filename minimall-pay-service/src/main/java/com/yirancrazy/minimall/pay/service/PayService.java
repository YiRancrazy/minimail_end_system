package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付领域服务接口，定义支付单创建与异步回调的领域契约，由 Controller 调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface PayService {
    Long create(Long orderId, BigDecimal amount);

    boolean callback(Long payId, boolean ok);
}