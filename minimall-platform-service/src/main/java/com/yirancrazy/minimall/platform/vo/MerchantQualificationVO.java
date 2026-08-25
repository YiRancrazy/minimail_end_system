package com.yirancrazy.minimall.platform.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家资质视图，列表与详情共用。merchantId 与 userId 1:1 对齐（商家主体即用户），
 *              引入 merchantId 字段以让前端在调用 /audit 端点时无需再回查。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantQualificationVO {

    /** 资质记录主键 */
    private Long id;

    /** 商家主体ID（与 userId 等值） */
    private Long merchantId;

    /** 商家名称 */
    private String merchantName;

    /** 证照编号 */
    private String licenseNo;

    /** 法人姓名 */
    private String legalPerson;

    /** 经营范围 */
    private String businessScope;

    /** 证照图片 URL */
    private String licenseImageUrl;

    /** 状态：PENDING/APPROVED/REJECTED */
    private String status;

    /** 驳回原因 */
    private String rejectReason;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 审核时间 */
    private LocalDateTime auditTime;
}
