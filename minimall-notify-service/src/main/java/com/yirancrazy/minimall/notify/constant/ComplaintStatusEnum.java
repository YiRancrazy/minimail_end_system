package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉状态枚举，code 持久化为 TINYINT
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum ComplaintStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待处理"),
    PROCESSING(1, "PROCESSING", "处理中"),
    RESOLVED(2, "RESOLVED", "已解决"),
    REJECTED(3, "REJECTED", "已驳回");

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
