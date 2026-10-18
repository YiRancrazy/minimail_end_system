package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantCodeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
public enum MerchantCodeEnum implements BaseEnum {
    MERCHANT_NOT_FOUND("15002", "MERCHANT_NOT_FOUND", "商家资质不存在"),
    MERCHANT_ALREADY_AUDITED("15003", "MERCHANT_ALREADY_AUDITED", "商家资质已审核，不可重复审核");

    private final String code;
    private final String alias;
    private final String message;
}
