package com.yirancrazy.minimall.pay.handle;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import com.yirancrazy.minimall.pay.manager.PayManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按订单号回放首次支付创建结果。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:14
 */
@Component
@RequiredArgsConstructor
public class PayCreateReplayHandler implements IdempotencyReplayHandler {

    public static final String REPLAY_TYPE = "pay:create";

    private final PayManager payManager;

    /**
     * 返回支付创建的业务类型。
     *
     * @return 支付创建业务类型
     */
    @Override
    public String replayType() {
        return REPLAY_TYPE;
    }

    /**
     * 查询订单号对应的最新支付流水并返回其主键。
     *
     * @param context 幂等上下文，其幂等键为业务订单号
     * @return 支付单 ID；记录尚未可见时返回 null
     */
    @Override
    public Result<Long> onDuplicate(IdempotencyContext context) {
        PayTransactionPo payment =
                payManager.findLatestByOrderNo(context.getIdempotencyKey());
        return payment == null ? null : Result.success(payment.getId());
    }
}
