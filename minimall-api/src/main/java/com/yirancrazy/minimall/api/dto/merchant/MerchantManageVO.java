package com.yirancrazy.minimall.api.dto.merchant;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家管理 VO，供跨服务调用使用
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantManageVO {

    private Long merchantId;
    private Long userId;
    private String merchantName;
    private String licenseNo;
    /** 营业执照图片 objectKey，内部链路透传原始 key，由对浏览器出参方解析 */
    private String licenseImageUrl;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;
    private LocalDateTime createTime;
}
