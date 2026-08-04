package com.yirancrazy.minimall.auth.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthUser持久化对象，映射t_auth_user表
 * @Version: 2.0
 * @DateTime: 2026/08/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_user")
public class AuthUserPO extends BasePO {
    private String account;

    private byte[] phoneEnc;

    private byte[] emailEnc;

    private String passwordHash;

    private String salt;

    private Integer accountType;

    private Long roleId;

    private Integer status;

    private String nickname;

    private LocalDateTime lastLoginAt;
}
