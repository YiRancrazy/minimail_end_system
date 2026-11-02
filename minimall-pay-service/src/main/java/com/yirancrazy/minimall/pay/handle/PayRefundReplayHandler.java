package com.yirancrazy.minimall.pay.handle;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按退款请求号回放已完成的退款结果。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:15
 */
@Component
@RequiredArgsConstructor
public class PayRefundReplayHandler implements IdempotencyReplayHandler {

    public static final String REPLAY_TYPE = "pay:refund";

    private final PayRefundManager payRefundManager;

    /**
     * 返回退款业务类型。
     *
     * @return 退款业务类型
     */
    @Override
    public String replayType() {
        return REPLAY_TYPE;
    }

    /**
     * 查询退款请求号对应的最终状态结果。
     *
     * @param context 幂等上下文，其幂等键为退款请求号
     * @return 已完成退款返回成功；待处理或不存在返回 null
     */
    @Override
    public Result<Boolean> onDuplicate(IdempotencyContext context) {
        PayRefundPo refund =
                payRefundManager.findByRefundRequestNo(context.getIdempotencyKey());
        if (refund == null
                || refund.getStatus() == null
                || refund.getStatus()
                        == Integer.parseInt(RefundStatusEnum.PENDING.getCode())) {
            return null;
        }
        return Result.success(true);
    }
}
