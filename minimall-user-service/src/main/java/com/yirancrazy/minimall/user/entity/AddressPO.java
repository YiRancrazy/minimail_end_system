package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址持久化对象，映射 t_user_address 表。收货人姓名、手机号和详细地址使用AES-256-GCM字段级加密。
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_user_address", autoResultMap = true)
public class AddressPO extends BasePO {

    /** 用户ID */
    private Long userId;

    /** 收货人姓名（加密存储） */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String receiverName;

    /** 收货人手机号（加密存储） */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址（加密存储） */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String detailAddress;

    /** 是否默认地址 0=否 1=是 */
    private Integer isDefault;
}
