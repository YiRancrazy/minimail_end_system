package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;

public interface PayService {
    Long create(Long orderId, BigDecimal amount);

    boolean callback(Long payId, boolean ok);
}