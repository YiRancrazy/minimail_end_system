package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址持久化模型，映射 t_user_address；收件人、手机号和详细地址以密文存储，默认标记使用 1 或 NULL.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_user_address", autoResultMap = true)
public class AddressPo extends BasePo {

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
