package com.yirancrazy.minimall.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台审核商家资质入参。
 * @Version: 1.0
 * @DateTime: 2026/08/09
 **/
@Data
public class MerchantQualificationAuditDTO {

    @NotNull(message = "资质ID不能为空")
    private Long qualificationId;

    /** true=通过，false=驳回。 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /** 驳回原因，approved=false 时必填。 */
    private String reason;
}