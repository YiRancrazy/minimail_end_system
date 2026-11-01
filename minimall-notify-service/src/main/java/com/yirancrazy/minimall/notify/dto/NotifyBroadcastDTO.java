package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台广播通知的接收方、消息类型、标题、正文、发送方和业务幂等键；指定 targetId 时定向发送，否则写入全端占位记录.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class NotifyBroadcastDto {

    @NotNull(message = "recipientType cannot be null")
    private Integer recipientType;

    @NotNull(message = "messageType cannot be null")
    private Integer messageType;

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;

    private Long senderId;

    private String bizId;

    private Long targetId;
}
