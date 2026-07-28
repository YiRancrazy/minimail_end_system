package com.yirancrazy.minimall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.entity.PayRecordPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.service.PayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PayServiceImpl implements PayService {

    private final PayManager payManager;

    public PayServiceImpl(PayManager payManager) {
        this.payManager = payManager;
    }

    @Override
    public Long create(Long orderId, BigDecimal amount) {
        PayRecordPO p = new PayRecordPO();
        p.setOrderId(orderId);
        p.setAmount(amount == null ? BigDecimal.ZERO : amount);
        p.setStatus("PENDING");
        payManager.save(p);
        return p.getId();
    }

    @Override
    public boolean callback(Long payId, boolean ok) {
        PayRecordPO p = payManager.getOne(
            Wrappers.lambdaQuery(PayRecordPO.class).eq(PayRecordPO::getId, payId));
        if (p == null) {
            throw new BizException("17001", "PAY_NOT_FOUND", "支付单不存在");
        }
        p.setStatus(ok ? "PAID" : "FAILED");
        return payManager.updateById(p);
    }
}