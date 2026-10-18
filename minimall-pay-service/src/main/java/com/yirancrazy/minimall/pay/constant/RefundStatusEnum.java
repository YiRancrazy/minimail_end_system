package com.yirancrazy.minimall.pay.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: RefundStatusEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
public enum RefundStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待发起"),
    SUCCESS(1, "SUCCESS", "退款成功"),
    FAILED(2, "FAILED", "退款失败"),
    CLOSED(3, "CLOSED", "已关闭");

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
}