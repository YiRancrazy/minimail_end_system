package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺创建请求；限定店铺名称长度和统一社会信用代码格式，商家归属由可信请求头决定而非请求体传入。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class ShopCreateDto {

  @NotBlank(message = "shopName cannot be blank")
  @Size(min = 1, max = 100, message = "shopName length must be between 1 and 100")
  private String shopName;

  @NotBlank(message = "licenseNo cannot be blank")
  @Pattern(regexp = "^[A-Z0-9]{15,20}$", message = "licenseNo format is invalid")
  private String licenseNo;
}
