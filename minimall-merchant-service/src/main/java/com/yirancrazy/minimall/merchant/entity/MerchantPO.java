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
 * @Description: 商家主体持久化对象，映射 t_merch_merchant 表。法人/身份证/银行卡等敏感字段 AES-256-GCM 加密存储（_enc 列）。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_merch_merchant", autoResultMap = true)
public class MerchantPO extends BasePO {
    private Long userId;
    private String merchantName;
    private String licenseNo;
    /** 营业执照图片 objectKey，出参时由 MinioUtil 解析为可访问 URL */
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
