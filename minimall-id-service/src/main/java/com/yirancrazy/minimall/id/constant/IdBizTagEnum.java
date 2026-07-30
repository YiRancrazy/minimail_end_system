package com.yirancrazy.minimall.id.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
* 业务标签枚举，定义订单、支付、库存、用户等业务域对应的号段 key，统一管理系统全局号段类型。
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