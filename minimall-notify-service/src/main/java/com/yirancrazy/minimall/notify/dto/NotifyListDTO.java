package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyList数据传输对象，用于NotifyList相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class NotifyListDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}