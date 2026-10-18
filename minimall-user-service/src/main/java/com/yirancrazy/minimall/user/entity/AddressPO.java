package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_user_address", autoResultMap = true)
public class AddressPO extends BasePO {


    private Long userId;


    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String receiverName;


    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String receiverPhone;


    private String province;


    private String city;


    private String district;


    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String detailAddress;


    @TableField(value = "is_default", insertStrategy = FieldStrategy.IGNORED)
    private Integer isDefault;
}
