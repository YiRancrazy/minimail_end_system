package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMessage持久化对象，映射t_notify_message站内信表
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_message")
public class NotifyMessagePO extends BasePO {

    /** 接收者ID（USER=userId, MERCHANT=merchantId, PLATFORM=adminId） */
    private Long userId;

    /** 接收方类型，见 RecipientTypeEnum */
    private Integer recipientType;

    /** 消息类型，见 NotifyMessageTypeEnum */
    private Integer messageType;

    /** 发送者ID，系统消息为null */
    private Long senderId;

    /** 业务关联ID，如订单号 */
    private String bizId;

    private String title;
    private String content;

    /** 已读标记 0=未读 1=已读 */
    private Integer readFlag;
}
