package com.yirancrazy.minimall.platform.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家列表视图对象
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformMerchantVO {

    private Long merchantId;
    private Long userId;
    private String merchantName;
    private String licenseNo;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;
    private LocalDateTime createTime;
}
