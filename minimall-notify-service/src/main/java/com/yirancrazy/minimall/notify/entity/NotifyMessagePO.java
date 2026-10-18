package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMessagePO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_message")
public class NotifyMessagePO extends BasePO {


    private Long userId;


    private Integer recipientType;


    private Integer messageType;


    private Long senderId;


    private String bizId;

    private String title;
    private String content;


    private Integer readFlag;
}
