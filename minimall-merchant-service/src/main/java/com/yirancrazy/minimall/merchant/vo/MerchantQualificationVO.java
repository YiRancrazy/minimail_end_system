package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质详情视图；输出主体、证照、审核结果和脱敏敏感字段，证照地址由服务层解析为可访问 URL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
public class MerchantQualificationVo {
  private Long id;

  private Long merchantId;
  private Long userId;
  private String merchantName;
  private String licenseNo;

  private String licenseImageUrl;
  private Integer auditStatus;
  private String auditReason;
  private LocalDateTime auditAt;
  private String legalPhoneMasked;
  private String idCardNoMasked;
  private String bankAccountMasked;
}
