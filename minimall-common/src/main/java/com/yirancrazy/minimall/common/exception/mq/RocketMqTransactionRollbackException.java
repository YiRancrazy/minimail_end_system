package com.yirancrazy.minimall.common.exception.mq;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 事务消息回调返回 ROLLBACK_MESSAGE 时抛出的异常，携带事件类型对应 tag 和事件对象，
 * 供调用方判断本地事务已回滚并执行补偿或重试。
 * @Version: 1.0
 * @DateTime: 2026/10/25 9:29
 */
@Setter
@Getter
public class RocketMqTransactionRollbackException extends RuntimeException {
    private final String tag;
    private final transient Object event;

    /**
     * 创建本地事务回滚异常.
     *
     * @param tag 消息标签
     * @param event 事务事件
     * @param cause 原始异常
     */
    public RocketMqTransactionRollbackException(String tag, Object event, Throwable cause) {
        super("RocketMQ 本地事务回滚，event=" + event.getClass().getSimpleName() + ", tag=" + tag, cause);
        this.tag = tag;
        this.event = event;
    }
}
