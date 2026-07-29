package com.yirancrazy.minimall.api.dto.notify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知事件 DTO，承载接收者、类型、标题与内容，供 notify-service 跨域消费。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyEventDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotBlank(message = "title cannot be blank")
    @Size(min = 1, max = 100, message = "title length must be between 1 and 100")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;
}