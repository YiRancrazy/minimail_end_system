package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyBroadcastDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class NotifyBroadcastDTO {


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
