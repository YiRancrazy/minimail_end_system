package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户认证持久化实体，对应 UserAuth 表，承载用户名、密码哈希、盐与角色。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_auth")
public class UserAuthPO extends BasePO {
    private String username;
    private String passwordHash;
    private String salt;
    private String role;
    private Integer status;
}