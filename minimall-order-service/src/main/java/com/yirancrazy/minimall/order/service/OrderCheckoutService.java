package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDto;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 编排用户结算来源解析、订单创建与购物车清理触发。
 *   业务边界：购物车来源读取服务端快照，立即购买跳过购物车调用；最终订单事务由 OrderService 承担。
 *   使用场景：UserOrderControllerV1.checkout 调用。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
public interface OrderCheckoutService {

    /**
     * 提交用户结算请求。
     *
     * <p>用户 ID 取自请求头，结算来源和明细由 OrderCheckoutService 校验并创建订单。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 用户结算请求
     * @param idempotencyKey 幂等键
     * @return 新订单 ID 的统一响应。
     * @throws BizException 结算参数、快照、库存或订单创建失败。
     */
    Long checkout(Long userId, OrderCheckoutDto dto, String idempotencyKey);
}
