package com.yirancrazy.minimall.common.event;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事务消息本地状态存储抽象，用于记录业务事务提交结果并支持 RocketMQ broker 回查。
 * 实现必须保证 Outbox 记录与业务数据在同一数据库事务中提交。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OutboxStore {

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 本地事务的提交状态，区分已提交、已回滚、无记录和未知状态，
     * 由 RocketMQ 事务回查映射为 COMMIT、ROLLBACK 或 UNKNOW。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    enum CommitStatus {
        COMMITTED,
        ROLLED_BACK,
        NOT_FOUND,
        UNKNOWN
    }

    /**
     * 记录事务消息对应的本地事务已提交.
     *
     * @param messageKey 消息键
     */
    void recordCommit(String messageKey);

    /**
     * 判断事务消息对应的本地事务是否已提交.
     *
     * @param messageKey 消息键
     * @return 是否已提交
     */
    boolean isCommitted(String messageKey);

    /**
     * 查询本地事务状态。 Outbox 记录必须与业务状态在同一本地事务中提交；查询不到记录表示本地事务未提交.
     *
     * @param messageKey 消息键
     * @return 提交状态
     */
    default CommitStatus commitStatus(String messageKey) {
        return isCommitted(messageKey) ? CommitStatus.COMMITTED : CommitStatus.NOT_FOUND;
    }
}
