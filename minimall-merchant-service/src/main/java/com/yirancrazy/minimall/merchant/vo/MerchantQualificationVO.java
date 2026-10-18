package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantQualificationVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
public class MerchantQualificationVO {
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
