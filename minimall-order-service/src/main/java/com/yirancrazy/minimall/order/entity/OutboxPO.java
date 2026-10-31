package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order_outbox，保存订单领域事件的可靠投递记录。
 *   业务边界：topic、tag、payload、发送状态、重试时间和错误信息构成发件箱协议；事件业务内容存放在 payload。
 *   使用场景：支付事务内写入，发布器和补偿任务异步投递。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_outbox")
public class OutboxPo extends BasePo {

  private String eventId; // 唯一业务事件键，例如 ORDER_PAID:<orderId>

  private String eventType; // 事件类型，例如 OrderPaidDto

  private String aggregateType; // 聚合类型，例如 ORDER

  private Long aggregateId; // 聚合标识，例如 orderId

  private String topic; // RocketMQ 主题

  private String tag; // RocketMQ 标签

  private String payload; // 事件 JSON 内容

  private Integer sendStatus; // 发送状态：0=待发送，1=已发送，2=死信

  private Integer retryCount; // 发送重试次数

  private LocalDateTime nextRetryTime; // 下次发送时间

  private String lastError; // 最近一次发送失败原因

  private LocalDateTime sentTime; // 发送成功时间

  // id、version、isDeleted、createTime、updateTime 继承自 BasePo
}
