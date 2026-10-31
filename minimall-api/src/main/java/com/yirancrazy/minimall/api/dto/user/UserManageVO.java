package com.yirancrazy.minimall.api.dto.user;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户管理视图，展示脱敏联系方式和基础资料.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserManageVo {

  private Long id;
  private String nickname;
  private String avatar;
  private String phoneMasked;
  private Integer gender;
  private LocalDateTime createTime;
}
