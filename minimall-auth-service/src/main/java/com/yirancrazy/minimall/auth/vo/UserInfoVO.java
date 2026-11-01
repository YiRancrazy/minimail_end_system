package com.yirancrazy.minimall.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 当前登录者的安全视图；区分认证账号 ID 与业务用户 ID，并携带 JWT 中的账号、角色信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
public class UserInfoVo {
    private Long authUserId;
    private Long userId;
    private String account;
    private String role;
    private Long roleId;
}
