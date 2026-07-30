package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
* 用户认证持久化实体，对应 UserAuth 表，承载用户名、密码哈希、盐与角色。
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