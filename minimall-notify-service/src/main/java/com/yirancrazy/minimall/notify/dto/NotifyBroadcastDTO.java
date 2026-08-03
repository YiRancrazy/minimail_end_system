package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台广播消息入参，支持全端或指定接收者推送站内信
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class NotifyBroadcastDTO {

    /** 目标接收方类型，见 RecipientTypeEnum */
    @NotNull(message = "recipientType cannot be null")
    private Integer recipientType;

    /** 消息类型，见 NotifyMessageTypeEnum */
    @NotNull(message = "messageType cannot be null")
    private Integer messageType;

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;

    /** 发送者ID（平台管理员ID），由 Header 注入 */
    private Long senderId;

    /** 业务关联ID */
    private String bizId;

    /** 指定接收者ID；null 表示全端广播（当前仅落库一条，按需扩展批量） */
    private Long targetId;
}
