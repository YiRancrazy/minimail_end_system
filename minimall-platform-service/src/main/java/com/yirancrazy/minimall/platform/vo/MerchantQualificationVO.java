package com.yirancrazy.minimall.platform.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质审核列表视图，聚合证照信息、审核状态及处理时间.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantQualificationVo {

  private Long id;

  private Long merchantId;

  private String merchantName;

  private String licenseNo;

  private String legalPerson;

  private String businessScope;

  private String licenseImageUrl;

  private String status;

  private String rejectReason;

  private LocalDateTime submitTime;

  private LocalDateTime auditTime;
}
