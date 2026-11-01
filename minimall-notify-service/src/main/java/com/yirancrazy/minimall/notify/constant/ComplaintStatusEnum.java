package com.yirancrazy.minimall.notify.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义投诉处理的流转状态；投诉服务仅允许待处理转处理中、处理中转已解决或已驳回，并以此约束条件更新.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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

    /**
     * 将投诉状态编码转换为 BaseEnum 约定的字符串形式.
     *
     * @return 投诉状态的十进制字符串编码
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 返回可直接用于数据库状态字段比较的整数编码.
     *
     * @return 投诉状态整数编码
     */
    public int intCode() {
        return code;
    }
}
