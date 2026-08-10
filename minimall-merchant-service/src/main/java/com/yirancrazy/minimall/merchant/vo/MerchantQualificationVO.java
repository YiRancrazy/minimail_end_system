package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质视图对象，返回资质与审核状态。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantQualificationVO {
    private Long id;
    /** 与 userId 同义，对外暴露稳定别名 merchantId。 */
    private Long merchantId;
    private Long userId;
    private String merchantName;
    private String licenseNo;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;
}
