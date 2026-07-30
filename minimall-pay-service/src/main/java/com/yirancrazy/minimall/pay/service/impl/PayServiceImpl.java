package com.yirancrazy.minimall.pay.service.impl;

import com.yirancrazy.minimall.pay.constant.PayCodeEnum;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
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

    /**
     * 创建支付单并持久化为待支付状态。
     *
     * @param orderId 关联的订单主键 ID
     * @param amount 支付金额；为空时按 0 处理
     * @return 新建支付单的主键 ID
     */
    @Override
    public Long create(Long orderId, BigDecimal amount) {
        PayTransactionPO p = new PayTransactionPO();
        p.setOrderNo(String.valueOf(orderId));
        p.setAmount(amount == null ? BigDecimal.ZERO : amount);
        p.setStatus(10);
        payManager.save(p);
        return p.getId();
    }

    /**
     * 处理支付回调，将支付单状态推进为已支付或失败。
     *
     * @param payId 支付单主键 ID
     * @param ok true 表示支付成功，false 表示支付失败
     * @return 数据库更新是否成功
     */
    @Override
    public boolean callback(Long payId, boolean ok) {
        PayTransactionPO p = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getId, payId));
        if (p == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        p.setStatus(ok ? 20 : 30);
        return payManager.updateById(p);
    }
}