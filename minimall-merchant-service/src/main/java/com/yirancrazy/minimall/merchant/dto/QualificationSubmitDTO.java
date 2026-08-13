package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质提交入参，含敏感字段（服务层加密落库，出参脱敏）。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 **/
@Data
public class QualificationSubmitDTO {

    @NotBlank(message = "merchantName must not be blank")
    @Size(max = 64, message = "merchantName must be <= 64 chars")
    private String merchantName;

    @NotBlank(message = "licenseNo must not be blank")
    @Size(max = 64, message = "licenseNo must be <= 64 chars")
    private String licenseNo;

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
