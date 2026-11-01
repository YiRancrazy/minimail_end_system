package com.yirancrazy.minimall.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质审核请求体，承载资质 ID、审核结果及拒绝原因.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class MerchantQualificationAuditDto {

    @NotNull(message = "资质ID不能为空")
    private Long qualificationId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    private String reason;
}
