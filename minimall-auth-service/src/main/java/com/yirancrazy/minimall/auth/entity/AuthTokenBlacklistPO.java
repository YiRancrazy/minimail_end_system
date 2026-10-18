package com.yirancrazy.minimall.auth.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthTokenBlacklistPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_token_blacklist")
public class AuthTokenBlacklistPO extends BasePO {
    private String jti;

    private Long userId;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private String reason;
}
