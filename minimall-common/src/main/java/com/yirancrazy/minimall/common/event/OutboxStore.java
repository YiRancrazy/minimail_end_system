package com.yirancrazy.minimall.common.event;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事务消息 outbox 持久化存储，记录半消息本地事务提交状态，供 broker 回查。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
public interface OutboxStore {

    /**
     * 本地事务提交状态，用于事务消息回查。
     */
    enum CommitStatus {
        PENDING,
        COMMITTED,
        ROLLED_BACK
    }

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

    /**
     * 查询本地事务状态。默认实现兼容只有提交记录的历史存储，未命中按待确认处理。
     * @param messageKey the RocketMQ transaction id or message key
     * @return 本地事务提交状态
     */
    default CommitStatus commitStatus(String messageKey) {
        return isCommitted(messageKey) ? CommitStatus.COMMITTED : CommitStatus.PENDING;
    }
}
