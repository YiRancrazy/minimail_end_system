package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家主体错误码枚举，定义资质审核相关错误码。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum MerchantCodeEnum implements BaseEnum {
    MERCHANT_NOT_FOUND("15002", "MERCHANT_NOT_FOUND", "商家资质不存在"),
    MERCHANT_ALREADY_AUDITED("15003", "MERCHANT_ALREADY_AUDITED", "商家资质已审核，不可重复审核"),
    WITHDRAW_NOT_FOUND("15010", "WITHDRAW_NOT_FOUND", "提现申请不存在"),
    WITHDRAW_STATUS_INVALID("15011", "WITHDRAW_STATUS_INVALID", "提现状态非法，禁止当前操作"),
    WITHDRAW_AMOUNT_INVALID("15012", "WITHDRAW_AMOUNT_INVALID", "提现金额必须大于 0"),
    MERCHANT_NOT_AUDITED("15013", "MERCHANT_NOT_AUDITED", "商家资质未审核通过，暂不可提现"),
    WITHDRAW_AMOUNT_EXCEED_LIMIT("15014", "WITHDRAW_AMOUNT_EXCEED_LIMIT", "提现金额超出上限");

    private final String code;
    private final String alias;
    private final String message;
}
