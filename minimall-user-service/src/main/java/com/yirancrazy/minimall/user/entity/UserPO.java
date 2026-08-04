package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: User 持久化对象，映射 t_user 表。phone和email字段使用AES-256-GCM字段级加密，phoneMasked存储脱敏值用于模糊查询。
 * @Version: 1.2
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_user", autoResultMap = true)
public class UserPO extends BasePO {
    private String username;
    private String nickname;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String phone;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String email;

    private String phoneMasked;

    private String avatar;
    private Integer gender;
}
