package com.yirancrazy.minimall.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址更新 DTO，用于修改收货地址入参校验
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class AddressUpdateDTO {

    @NotBlank(message = "receiver cannot be blank")
    @Size(max = 64, message = "receiver length must be <= 64")
    private String receiver;

    @NotBlank(message = "phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone format is invalid")
    private String phone;

    @NotBlank(message = "province cannot be blank")
    @Size(max = 32, message = "province length must be <= 32")
    private String province;

    @NotBlank(message = "city cannot be blank")
    @Size(max = 32, message = "city length must be <= 32")
    private String city;

    @NotBlank(message = "district cannot be blank")
    @Size(max = 32, message = "district length must be <= 32")
    private String district;

    @NotBlank(message = "detail cannot be blank")
    @Size(max = 256, message = "detail length must be <= 256")
    private String detail;
}
