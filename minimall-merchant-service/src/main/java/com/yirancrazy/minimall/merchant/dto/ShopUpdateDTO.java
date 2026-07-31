package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopUpdate数据传输对象，用于ShopUpdate相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class ShopUpdateDTO {

    @NotBlank(message = "shopName cannot be blank")
    @Size(min = 1, max = 100, message = "shopName length must be between 1 and 100")
    private String shopName;

    @NotBlank(message = "licenseNo cannot be blank")
    @Pattern(regexp = "^[A-Z0-9]{15,20}$", message = "licenseNo format is invalid")
    private String licenseNo;

    @NotBlank(message = "status cannot be blank")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED)$", message = "status must be ACTIVE, INACTIVE or SUSPENDED")
    private String status;
}