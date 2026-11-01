package com.yirancrazy.minimall.order.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义订单事务性发件箱事件的批量发布能力。
 *   业务边界：发布器负责抢占、投递、重试和死信推进，不负责在业务事务内写入事件。
 *   使用场景：定时任务按批调用以可靠投递订单支付事件。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
public interface OrderOutboxPublisher {

    /**
     * 批量发布到期的订单发件箱事件。
     *
     * <p>每批先查询候选并用数据库条件抢占，投递成功标记已发送，失败按指数退避重试或进入死信；返回仅统计成功发送数。
     *
     * @param limit 单批最大处理数量
     * @return 本轮成功发送的事件数量。
     */
    int publishPending(int limit);
}
