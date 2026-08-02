package com.yirancrazy.minimall.common.event;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事务消息 outbox 持久化存储，记录半消息本地事务提交状态，供 broker 回查。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
public interface OutboxStore {

    /**
     * Record that the local transaction for the given message committed.
     * @param messageKey the RocketMQ transaction id or message key
     */
    void recordCommit(String messageKey);

    /**
     * Probe whether the local transaction for the given message committed.
     * @param messageKey the RocketMQ transaction id or message key
     * @return true if committed; false otherwise
     */
    boolean isCommitted(String messageKey);
}
