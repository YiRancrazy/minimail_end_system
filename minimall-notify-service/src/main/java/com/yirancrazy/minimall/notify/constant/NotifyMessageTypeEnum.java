package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知消息类型枚举，覆盖订单/物流/退款/营销/公告/系统/违规场景
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum NotifyMessageTypeEnum implements BaseEnum {
    ORDER(1, "ORDER", "订单"),
    LOGISTICS(2, "LOGISTICS", "物流"),
    REFUND(3, "REFUND", "退款"),
    PROMOTION(4, "PROMOTION", "营销"),
    ANNOUNCEMENT(5, "ANNOUNCEMENT", "公告"),
    SYSTEM(6, "SYSTEM", "系统"),
    VIOLATION(7, "VIOLATION", "违规");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * Get the integer code for persistence.
     * @return the integer code value
     */
    public int intCode() {
        return code;
    }
}
