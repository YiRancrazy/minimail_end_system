package com.yirancrazy.minimall.common.exception.mq;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: RocketMQ 本地事务回滚异常
 * @Version: 1.0
 * @DateTime: 2026/10/25 9:29
 **/

@Setter
@Getter
public class RocketMqTransactionRollbackException extends RuntimeException{
    private final String tag;
    private final transient Object event;

    public RocketMqTransactionRollbackException(String tag, Object event, Throwable cause) {
        super("RocketMQ 本地事务回滚，event=" + event.getClass().getSimpleName() + ", tag=" + tag, cause);
        this.tag = tag;
        this.event = event;
    }
}
