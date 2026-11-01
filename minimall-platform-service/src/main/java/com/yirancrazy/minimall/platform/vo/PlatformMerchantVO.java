package com.yirancrazy.minimall.platform.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家管理与详情视图，承载商家身份、证照及审核结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformMerchantVo {

    private Long merchantId;
    private Long userId;
    private String merchantName;
    private String licenseNo;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;
    private LocalDateTime createTime;
}
