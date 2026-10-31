package com.yirancrazy.minimall.order.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.event.OutboxStore;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将通用 OutboxStore 的消息键适配到订单发件箱持久化表。
 *   业务边界：只实现提交标记和查询，不负责事件载荷构造、路由或补偿调度。
 *   使用场景：通用事件组件需要确认消息键是否已提交时使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
public class JdbcOutboxStore implements OutboxStore {

  private final OutboxManager outboxManager;

  /**
   * 创建 JDBC 发件箱存储适配器。
   *
   * <p>构造器注入 OutboxManager，后续提交标记和查询都通过该 Manager 访问持久化表。
   *
   * @param outboxManager 发件箱记录持久化与条件更新入口
   */
  public JdbcOutboxStore(OutboxManager outboxManager) {
    this.outboxManager = outboxManager;
  }

  /**
   * 登记通用消息键的提交标记。
   *
   * <p>只保存 eventId 占位记录；重复键按幂等忽略，不构造订单事件。
   *
   * @param messageKey 事件或消息的稳定幂等键
   */
  public void recordCommit(String messageKey) {
    OutboxPo po = new OutboxPo();
    po.setEventId(messageKey);
    try {
      outboxManager.save(po);
    } catch (DuplicateKeyException e) {
      // ignore duplicate event records for idempotency
    }
  }

  /**
   * 判断消息键是否已写入发件箱提交记录。
   *
   * <p>按 eventId 计数，大于零即视为已提交。
   *
   * @param messageKey 事件或消息的稳定幂等键
   * @return 存在记录时返回 true。
   */
  @Override
  public boolean isCommitted(String messageKey) {
    long count =
        outboxManager.count(
            Wrappers.lambdaQuery(OutboxPo.class).eq(OutboxPo::getEventId, messageKey));
    return count > 0;
  }
}
