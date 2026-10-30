package com.yirancrazy.minimall.order.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单事务性发件箱持久化对象，用于可靠异步投递订单事件。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_outbox")
public class OutboxPO extends BasePO {

    private String eventId; // 唯一业务事件键，例如 ORDER_PAID:<orderId>

    private String eventType; // 事件类型，例如 OrderPaidDTO

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

    // id、version、isDeleted、createTime、updateTime 继承自 BasePO
}
