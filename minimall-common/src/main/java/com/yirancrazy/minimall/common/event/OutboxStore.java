package com.yirancrazy.minimall.common.event;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OutboxStore 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OutboxStore {


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: CommitStatus 枚举，定义相关业务状态。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    enum CommitStatus {
        PENDING,
        COMMITTED,
        ROLLED_BACK
    }

    /**
     * 记录本地事务提交。
     * @param messageKey 消息键
     */
    void recordCommit(String messageKey);


    /**
     * 判断本地事务是否已提交。
     * @param messageKey 消息键
     * @return 是否已提交
     */
    boolean isCommitted(String messageKey);


    /**
     * 查询消息提交状态。
     * @param messageKey 消息键
     * @return 提交状态
     */
    default CommitStatus commitStatus(String messageKey) {
        return isCommitted(messageKey) ? CommitStatus.COMMITTED : CommitStatus.PENDING;
    }
}
