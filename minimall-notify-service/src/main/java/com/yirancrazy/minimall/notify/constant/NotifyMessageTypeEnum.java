package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMessageTypeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }


    /**
     * 执行 intCode 方法。
     * @return 业务数值
     */
    public int intCode() {
        return code;
    }
}
