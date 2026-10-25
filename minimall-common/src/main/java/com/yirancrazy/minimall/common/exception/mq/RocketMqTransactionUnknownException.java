package com.yirancrazy.minimall.common.exception.mq;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: RocketMq 事务未知异常
 * @Version: 1.0
 * @DateTime: 2026/10/25 9:31
 **/

@Setter
@Getter
public class RocketMqTransactionUnknownException extends RuntimeException {
    private final String tag;
    private final transient Object event;

    public RocketMqTransactionUnknownException(String tag, Object event, Throwable cause) {
        super("RocketMQ 本地事务状态未知，event=" + event.getClass().getSimpleName() + ", tag=" + tag, cause);
        this.tag = tag;
        this.event = event;
    }
}
