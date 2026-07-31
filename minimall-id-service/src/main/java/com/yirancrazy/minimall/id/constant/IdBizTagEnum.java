package com.yirancrazy.minimall.id.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID生成IdBizTag枚举，定义ID生成相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter
@AllArgsConstructor
public enum IdBizTagEnum implements BaseEnum {
    ORDER("order", "ORDER_ID", "订单"),
    PAY("pay", "PAY_ID", "支付"),
    STOCK("stock", "STOCK_ID", "库存"),
    USER("user", "USER_ID", "用户");

    private final String code;
    private final String alias;
    private final String message;
}