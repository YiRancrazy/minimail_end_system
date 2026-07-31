package com.yirancrazy.minimall.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserSnapshot数据传输对象，用于UserSnapshot相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class UserSnapshotDTO {
    private Long userId;
    private String username;
    private String role;
}