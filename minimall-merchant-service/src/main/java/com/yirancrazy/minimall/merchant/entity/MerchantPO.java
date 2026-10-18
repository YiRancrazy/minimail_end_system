package com.yirancrazy.minimall.merchant.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_merch_merchant", autoResultMap = true)
public class MerchantPO extends BasePO {
    private Long userId;
    private String merchantName;
    private String licenseNo;

    private String licenseImageUrl;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String legalPersonEnc;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String legalPhoneEnc;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String idCardNoEnc;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String businessLicenseNoEnc;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String bankAccountEnc;
}
