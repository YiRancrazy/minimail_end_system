package com.yirancrazy.minimall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱事件发送状态。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@Getter
@AllArgsConstructor
public enum OutboxSendStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待发送"),
    SENT(1, "SENT", "已发送"),
    DEAD(2, "DEAD", "死信");

    /**
     * 状态码。
     */
    private final int code;

    /**
     * 状态别名。
     */
    private final String alias;

    /**
     * 状态说明。
     */
    private final String message;

    /**
     * 获取字符串状态码。
     *
     * @return 字符串状态码
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 获取整数状态码。
     *
     * @return 整数状态码
     */
    public int intCode() {
        return code;
    }
}
