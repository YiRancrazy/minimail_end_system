package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证账号持久化对象，映射 t_auth_user；保存登录凭据、账号类型、角色和关联业务用户 ID。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_user")
public class AuthUserPo extends BasePo {
    private Long userId;

    private String account;

    private String passwordHash;

    private String salt;

    private Integer accountType;

    private Long roleId;

    private Integer status;

    private String contact;

    private LocalDateTime lastLoginAt;
}
