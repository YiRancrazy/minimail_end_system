package com.yirancrazy.minimall.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserSnapshotDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSnapshotDTO {
    private Long userId;
    private String username;
    private String role;
}