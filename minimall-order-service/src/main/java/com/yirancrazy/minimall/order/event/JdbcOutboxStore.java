package com.yirancrazy.minimall.order.event;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.event.OutboxStore;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: JdbcOutboxStore 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
public class JdbcOutboxStore implements OutboxStore {

    private final OutboxManager outboxManager;

    /**
     * 构造 JdbcOutboxStore 实例。
     * @param outboxManager outboxManager 参数
     */
    public JdbcOutboxStore(OutboxManager outboxManager) {
        this.outboxManager = outboxManager;
    }


    /**
     * 执行 recordCommit 方法。
     * @param messageKey messageKey 参数
     */
    public void recordCommit(String messageKey) {
        OutboxPO po = new OutboxPO();
        po.setTransactionId(messageKey);
        try {
            outboxManager.save(po);
        }
        catch (DuplicateKeyException e) {
            // 幂等：已记录
        }
    }


    /**
     * 执行 isCommitted 方法。
     * @param messageKey messageKey 参数
     * @return 是否处理成功
     */
    @Override
    public boolean isCommitted(String messageKey) {
        long count = outboxManager.count(Wrappers.lambdaQuery(OutboxPO.class)
            .eq(OutboxPO::getTransactionId, messageKey));
        return count > 0;
    }
}
