package com.yirancrazy.minimall.auth.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthTokenBlacklist持久化对象，映射t_auth_token_blacklist表
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Data
@TableName("t_auth_token_blacklist")
public class AuthTokenBlacklistPO {
    private String jti;

    private Long userId;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private String reason;
}
