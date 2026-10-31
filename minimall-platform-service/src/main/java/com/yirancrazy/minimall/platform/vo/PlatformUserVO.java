package com.yirancrazy.minimall.platform.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户管理视图，提供基础资料、脱敏手机号及可访问头像地址.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUserVo {

  private Long id;
  private String nickname;
  private String avatar;
  private String phoneMasked;
  private Integer gender;
  private LocalDateTime createTime;
}
