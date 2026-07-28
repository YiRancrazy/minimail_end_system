package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

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