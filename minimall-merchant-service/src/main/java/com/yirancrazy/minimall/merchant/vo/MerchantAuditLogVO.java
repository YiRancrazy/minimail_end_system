package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家审核记录视图对象，包含资质与商品审核日志。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class MerchantAuditLogVO {
    private Long id;
    private String auditType;
    private Long targetId;
    private Integer decision;
    private String reason;
    private Long auditorId;
    private LocalDateTime auditAt;
}
