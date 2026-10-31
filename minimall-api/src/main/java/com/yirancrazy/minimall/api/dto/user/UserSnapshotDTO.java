package com.yirancrazy.minimall.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 其他服务读取的用户身份快照，包含用户 ID、昵称和角色.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSnapshotDto {
  private Long userId;
  private String nickname;
  private String role;
}
