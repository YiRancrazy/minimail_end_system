package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 撤销记录持久化对象，映射 t_auth_token_blacklist；按 jti 保存失效时间和撤销原因。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_token_blacklist")
public class AuthTokenBlacklistPo extends BasePo {
  private String jti;

  private Long userId;

  private LocalDateTime expiresAt;

  private LocalDateTime revokedAt;

  private String reason;
}
