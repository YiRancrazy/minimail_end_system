package com.yirancrazy.minimall.common.event;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 本地消息表存储接口，用于记录并回查事务消息对应本地事务的提交状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OutboxStore {


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 本地事务的提交状态，用于事务消息回查。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    enum CommitStatus {
        PENDING,
        COMMITTED,
        ROLLED_BACK
    }

    /**
     * 记录事务消息对应的本地事务已提交。
     *
     * @param messageKey 消息键
     */
    void recordCommit(String messageKey);


    /**
     * 判断事务消息对应的本地事务是否已提交。
     *
     * @param messageKey 消息键
     * @return 是否已提交
     */
    boolean isCommitted(String messageKey);


    /**
     * 查询本地事务状态。
     * 默认实现仅区分已提交和待定。
     *
     * @param messageKey 消息键
     * @return 提交状态
     */
    default CommitStatus commitStatus(String messageKey) {
        return isCommitted(messageKey) ? CommitStatus.COMMITTED : CommitStatus.PENDING;
    }
}
