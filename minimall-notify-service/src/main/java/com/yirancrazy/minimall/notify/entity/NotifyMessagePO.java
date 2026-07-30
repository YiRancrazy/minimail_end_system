package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
* 通知消息持久化实体，承载用户、类型、标题、内容与已读状态，映射至 t_notify_message 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_message")
public class NotifyMessagePO extends BasePO {
    private Long userId;
    private String title;
    private String content;
    private Integer readFlag;
}