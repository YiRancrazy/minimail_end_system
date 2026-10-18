package com.yirancrazy.minimall.api.dto.merchant;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantManageVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantManageVO {

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
