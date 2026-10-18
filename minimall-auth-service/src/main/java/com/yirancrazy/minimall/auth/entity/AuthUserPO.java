package com.yirancrazy.minimall.auth.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthUserPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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

    private String contact;

    private LocalDateTime lastLoginAt;
}
