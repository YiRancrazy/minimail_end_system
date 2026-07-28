package com.yirancrazy.minimall.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoVO {
    private Long userId;
    private String username;
    private String role;
}