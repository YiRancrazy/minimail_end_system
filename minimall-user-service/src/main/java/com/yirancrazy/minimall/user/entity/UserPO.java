package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户档案持久化模型，映射 t_user；手机号和邮箱使用加密类型处理器，另保存手机号脱敏值供查询展示.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_user", autoResultMap = true)
public class UserPo extends BasePo {
  private String nickname;

  @TableField(typeHandler = EncryptedStringTypeHandler.class)
  private String phone;

  @TableField(typeHandler = EncryptedStringTypeHandler.class)
  private String email;

  private String phoneMasked;

  private String avatar;
  private Integer gender;
}
