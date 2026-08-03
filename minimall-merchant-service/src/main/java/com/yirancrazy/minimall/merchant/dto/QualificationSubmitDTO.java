package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质提交入参。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class QualificationSubmitDTO {

    @NotBlank(message = "merchantName must not be blank")
    @Size(max = 64, message = "merchantName must be <= 64 chars")
    private String merchantName;

    @NotBlank(message = "licenseNo must not be blank")
    @Size(max = 64, message = "licenseNo must be <= 64 chars")
    private String licenseNo;
}
