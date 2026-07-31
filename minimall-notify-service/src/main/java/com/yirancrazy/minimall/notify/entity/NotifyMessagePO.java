package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMessage持久化对象，映射notifymessage表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class NotifyMessagePO extends BasePO {
    private Long userId;
    private String title;
    private String content;
    private Integer readFlag;
}