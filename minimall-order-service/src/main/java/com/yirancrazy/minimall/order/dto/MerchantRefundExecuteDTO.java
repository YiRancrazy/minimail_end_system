package com.yirancrazy.minimall.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家发起退款所需的金额和原因。
 *   业务边界：金额以字符串接收并由服务层校验两位小数及剩余可退金额，原因用于退款申请记录。
 *   使用场景：MerchantRefundControllerV1 的 executeRefund 请求体使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class MerchantRefundExecuteDto {

  @NotBlank(message = "退款金额不能为空")
  @Pattern(regexp = "^(0|[1-9][0-9]*)(\\.\\d{1,2})?$", message = "退款金额必须是合法数字（最多两位小数）")
  private String refundAmount;

  @NotBlank(message = "退款原因不能为空")
  private String reason;
}
