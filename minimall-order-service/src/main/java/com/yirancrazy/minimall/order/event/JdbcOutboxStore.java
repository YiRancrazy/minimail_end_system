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
     * 记录半消息的提交。重复的 transactionId 插入被视为幂等提交，可以容忍。
     * @param messageKey RocketMQ 事务 ID
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
     * 查询半消息是否已提交。
     * @param messageKey RocketMQ 事务 ID
     * @return true 如果存在 transactionId 的记录
     */
    @Override
    public boolean isCommitted(String messageKey) {
        long count = outboxManager.count(Wrappers.lambdaQuery(OutboxPO.class)
            .eq(OutboxPO::getTransactionId, messageKey));
        return count > 0;
    }
}
