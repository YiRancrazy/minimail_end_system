package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Shop creation request DTO.
 */
@Data
public class ShopCreateDTO {

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