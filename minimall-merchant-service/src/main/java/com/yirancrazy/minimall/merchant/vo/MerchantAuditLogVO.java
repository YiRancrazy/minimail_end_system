package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家审核记录视图；输出审核类型、目标、决策、原因、审核人和时间，当前服务仅根据商家记录构造一条资质审核记录。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class MerchantAuditLogVo {
  private Long id;
  private String auditType;
  private Long targetId;
  private Integer decision;
  private String reason;
  private Long auditorId;
  private LocalDateTime auditAt;
}
