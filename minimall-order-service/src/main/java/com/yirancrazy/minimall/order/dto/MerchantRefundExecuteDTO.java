package com.yirancrazy.minimall.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家主动发起退款入参。refundAmount 使用字符串避免精度损失。
 * @Version: 1.0
 * @DateTime: 2026/08/09
 **/
@Data
public class MerchantRefundExecuteDTO {

    @NotBlank(message = "退款金额不能为空")
    @Pattern(regexp = "^(0|[1-9][0-9]*)(\\.\\d{1,2})?$", message = "退款金额必须是合法数字（最多两位小数）")
    private String refundAmount;

    @NotBlank(message = "退款原因不能为空")
    private String reason;
}