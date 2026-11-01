package com.yirancrazy.minimall.merchant.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质业务错误码；覆盖资质不存在和重复审核场景，由服务层转换为 BizException 返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum MerchantCodeEnum implements BaseEnum {
    MERCHANT_NOT_FOUND("15002", "MERCHANT_NOT_FOUND", "商家资质不存在"),
    MERCHANT_ALREADY_AUDITED("15003", "MERCHANT_ALREADY_AUDITED", "商家资质已审核，不可重复审核");

    private final String code;
    private final String alias;
    private final String message;
}
