package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_message")
public class NotifyMessagePO extends BasePO {
    private Long userId;
    private String title;
    private String content;
    private Integer readFlag;
}