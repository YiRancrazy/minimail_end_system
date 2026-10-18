package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantAuditLogVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
