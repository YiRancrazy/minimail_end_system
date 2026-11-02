package com.yirancrazy.minimall.order.handle;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.manager.OrderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 订单创建幂等处理
 * @Version: 1.0
 * @DateTime: 2026/11/1 19:47
 **/

@Component
@RequiredArgsConstructor
public class OrderCheckoutReplayHandler implements IdempotencyReplayHandler {

    private final OrderManager orderManager;

    public static final String REPLAY_TYPE = "order:checkout";

    /**
     * 获取幂等键的重试类型。
     *
     * @return 幂等键的重试类型
     */
    @Override
    public String replayType() {
        return REPLAY_TYPE;
    }

    /**
     * 处理重复创建订单请求，如果已经创建则返回订单 ID。
     *
     * @param context idempotency context
     * @return 订单 ID；订单尚未可见时返回 null
     */
    @Override
    public Result<Long> onDuplicate(IdempotencyContext context) {
        OrderPo orderPo = orderManager.getOrderByIdempotencyKey(context.getIdempotencyKey());
        return orderPo == null ? null : Result.success(orderPo.getId());
    }
}
