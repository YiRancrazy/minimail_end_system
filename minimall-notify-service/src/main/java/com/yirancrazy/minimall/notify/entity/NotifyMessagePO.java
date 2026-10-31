package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_notify_message 站内通知记录，保存接收方、消息类型、业务唯一键、展示内容和已读状态.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_message")
public class NotifyMessagePo extends BasePo {

  private Long userId;

  private Integer recipientType;

  private Integer messageType;

  private Long senderId;

  private String bizId;

  private String title;
  private String content;

  private Integer readFlag;
}
