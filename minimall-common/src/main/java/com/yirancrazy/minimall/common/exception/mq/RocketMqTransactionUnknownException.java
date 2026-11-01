package com.yirancrazy.minimall.common.exception.mq;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 事务消息本地状态无法确定时抛出的异常，表示 broker 可能通过 OutboxStore 回查最终状态。
 * 调用方不应直接假定事务成功或失败，而应依据业务幂等与查询结果处理。
 * @Version: 1.0
 * @DateTime: 2026/10/25 9:31
 */
@Setter
@Getter
public class RocketMqTransactionUnknownException extends RuntimeException {
    private final String tag;
    private final transient Object event;

    /**
     * 创建本地事务状态未知异常.
     *
     * @param tag 消息标签
     * @param event 事务事件
     * @param cause 原始异常
     */
    public RocketMqTransactionUnknownException(String tag, Object event, Throwable cause) {
        super("RocketMQ 本地事务状态未知，event=" + event.getClass().getSimpleName() + ", tag=" + tag, cause);
        this.tag = tag;
        this.event = event;
    }
}
