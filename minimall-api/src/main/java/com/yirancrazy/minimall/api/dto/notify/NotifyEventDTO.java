package com.yirancrazy.minimall.api.dto.notify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyEventDTO {
    private Long userId;
    private String title;
    private String content;
}