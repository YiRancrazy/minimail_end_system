package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
