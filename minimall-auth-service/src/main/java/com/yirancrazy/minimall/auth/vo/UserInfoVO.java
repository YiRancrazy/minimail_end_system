package com.yirancrazy.minimall.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 当前用户信息 VO，向客户端返回用户标识、用户名与角色。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@AllArgsConstructor
public class UserInfoVO {
    private Long userId;
    private String username;
    private String role;
}