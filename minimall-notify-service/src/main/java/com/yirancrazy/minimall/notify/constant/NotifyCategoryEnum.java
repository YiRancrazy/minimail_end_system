package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知类别枚举，code 持久化为 VARCHAR(32)
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Getter
@AllArgsConstructor
public enum NotifyCategoryEnum implements BaseEnum {
    ORDER("ORDER", "订单通知"),
    PAYMENT("PAYMENT", "支付通知"),
    REFUND("REFUND", "退款通知"),
    STOCK("STOCK", "库存通知"),
    COMMENT("COMMENT", "评价回复通知"),
    ACTIVITY("ACTIVITY", "活动通知"),
    SYSTEM("SYSTEM", "系统通知");

    private final String code;
    private final String message;

    @Override
    public String getAlias() {
        return code;
    }
}
