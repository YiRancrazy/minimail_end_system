package com.yirancrazy.minimall.api.dto.merchant;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家管理视图，汇总主体、资质、审核状态和时间.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantManageVo {

    private Long merchantId;
    private Long userId;
    private String merchantName;
    private String licenseNo;

    private String licenseImageUrl;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;
    private LocalDateTime createTime;
}
