package com.yirancrazy.minimall.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户跨服务快照 DTO，承载用户标识、用户名与角色，供他服务按快照消费。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSnapshotDTO {
    private Long userId;
    private String username;
    private String role;
}