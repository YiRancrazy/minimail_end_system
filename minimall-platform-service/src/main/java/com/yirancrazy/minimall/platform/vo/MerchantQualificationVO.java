package com.yirancrazy.minimall.platform.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class MerchantQualificationVO {


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
