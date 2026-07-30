package com.yirancrazy.minimall.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
* 当前用户信息 VO，向客户端返回用户标识、用户名与角色。
 */
@Data
@AllArgsConstructor
public class UserInfoVO {
    private Long userId;
    private String username;
    private String role;
}