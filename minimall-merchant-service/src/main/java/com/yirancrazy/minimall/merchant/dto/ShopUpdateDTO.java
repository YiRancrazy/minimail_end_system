package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺更新请求；要求提交店铺名称与证照号，可选 status 按 alias 转换，SUSPENDED 仅供平台管控。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class ShopUpdateDto {

    @NotBlank(message = "shopName cannot be blank")
    @Size(min = 1, max = 100, message = "shopName length must be between 1 and 100")
    private String shopName;

    @NotBlank(message = "licenseNo cannot be blank")
    @Pattern(regexp = "^[A-Z0-9]{15,20}$", message = "licenseNo format is invalid")
    private String licenseNo;

    // 状态可选：传值则校验变更，不传保持服务端既有管控值（SUSPENDED 为平台专用，商家无权设置）
    private String status;
}
