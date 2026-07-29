package com.yirancrazy.minimall.api.dto.notify;

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
    private Long userId;
    private String title;
    private String content;
}