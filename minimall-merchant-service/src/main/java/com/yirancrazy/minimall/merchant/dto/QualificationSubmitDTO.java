package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: QualificationSubmitDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class QualificationSubmitDTO {

    @NotBlank(message = "merchantName must not be blank")
    @Size(max = 64, message = "merchantName must be <= 64 chars")
    private String merchantName;

    @NotBlank(message = "licenseNo must not be blank")
    @Size(max = 64, message = "licenseNo must be <= 64 chars")
    private String licenseNo;

    @Size(max = 512, message = "licenseImageUrl must be <= 512 chars")
    private String licenseImageUrl;

    @Size(max = 64, message = "legalPerson must be <= 64 chars")
    private String legalPerson;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "legalPhone must be a valid mobile number")
    private String legalPhone;

    @Pattern(regexp = "^$|^\\d{15,18}$", message = "idCardNo must be 15-18 digits")
    private String idCardNo;

    @Size(max = 64, message = "businessLicenseNo must be <= 64 chars")
    private String businessLicenseNo;

    @Pattern(regexp = "^$|^\\d{8,30}$", message = "bankAccount must be 8-30 digits")
    private String bankAccount;
}
