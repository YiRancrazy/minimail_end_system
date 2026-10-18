package com.yirancrazy.minimall.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserInfoVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AllArgsConstructor
public class UserInfoVO {
    private Long userId;
    private String account;
    private String role;
    private Long roleId;
}
