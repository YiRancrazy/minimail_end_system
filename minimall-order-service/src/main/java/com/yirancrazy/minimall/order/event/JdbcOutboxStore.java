package com.yirancrazy.minimall.order.event;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.event.OutboxStore;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 t_order_outbox 表的 OutboxStore 实现，持久化事务消息提交状态供 broker 回查。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Component
public class JdbcOutboxStore implements OutboxStore {

    private final OutboxManager outboxManager;

    public JdbcOutboxStore(OutboxManager outboxManager) {
        this.outboxManager = outboxManager;
    }

    /**
     * Record the commit of a half-message. Duplicate transactionId inserts
     * are tolerated as idempotent commits.
     * @param messageKey the RocketMQ transaction id
     */
    @Override
    public void recordCommit(String messageKey) {
        OutboxPO po = new OutboxPO();
        po.setTransactionId(messageKey);
        try {
            outboxManager.save(po);
        }
        catch (DuplicateKeyException e) {
            // idempotent: already recorded
        }
    }

    /**
     * Probe whether a half-message committed.
     * @param messageKey the RocketMQ transaction id
     * @return true if a record exists for the transactionId
     */
    @Override
    public boolean isCommitted(String messageKey) {
        long count = outboxManager.count(Wrappers.lambdaQuery(OutboxPO.class)
            .eq(OutboxPO::getTransactionId, messageKey));
        return count > 0;
    }
}
