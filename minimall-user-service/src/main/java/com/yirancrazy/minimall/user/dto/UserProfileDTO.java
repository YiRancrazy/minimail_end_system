package com.yirancrazy.minimall.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户自助修改资料的局部更新请求，仅承载昵称、头像和性别字段.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class UserProfileDto {

  @Size(max = 100, message = "nickname length must be <= 100")
  private String nickname;

  @Size(max = 512, message = "avatar length must be <= 512")
  private String avatar;

  private Integer gender;
}
