package com.yirancrazy.minimall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单库存释放状态，区分无需释放、待释放与已释放。
 * @Version: 1.0
 * @DateTime: 2026/09/21
 **/
@Getter
@AllArgsConstructor
public enum StockReleaseStatusEnum implements BaseEnum {
    NOT_REQUIRED(0, "NOT_REQUIRED", "无需释放"),
    PENDING(1, "PENDING", "待释放"),
    RELEASED(2, "RELEASED", "已释放");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 获取持久化整数状态码。
     * @return 整数状态码
     */
    public int intCode() {
        return code;
    }
}
