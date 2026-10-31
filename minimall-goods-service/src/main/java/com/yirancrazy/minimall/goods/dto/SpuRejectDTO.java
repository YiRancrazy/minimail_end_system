package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台审核驳回商品时填写的必填原因。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class SpuRejectDto {

  @NotBlank(message = "reason cannot be blank")
  @Size(max = 512, message = "reason length must be <= 512")
  private String reason;
}
