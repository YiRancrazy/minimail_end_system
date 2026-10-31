package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质持久化对象，映射 t_merch_merchant；保存主体、证照、审核状态，以及经 EncryptedStringTypeHandler 加解密的法人、手机号、身份证和银行卡密文列。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_merch_merchant", autoResultMap = true)
public class MerchantPo extends BasePo {
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
