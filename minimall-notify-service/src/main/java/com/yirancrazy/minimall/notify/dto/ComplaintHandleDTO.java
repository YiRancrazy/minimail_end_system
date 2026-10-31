package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台处理投诉时指定的目标状态和处理结果，实际流转是否合法由投诉服务校验.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class ComplaintHandleDto {

  @NotNull(message = "status cannot be null")
  private Integer status;

  @NotBlank(message = "result cannot be blank")
  private String result;
}
