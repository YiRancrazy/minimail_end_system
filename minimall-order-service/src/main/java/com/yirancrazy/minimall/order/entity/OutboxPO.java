package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事务消息 outbox 持久化对象，记录已提交的半消息 transactionId。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_outbox")
public class OutboxPO extends BasePO {
    private String transactionId;
}
