package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyCategoryEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getAlias() {
        return code;
    }
}
