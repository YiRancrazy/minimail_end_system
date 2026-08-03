package com.yirancrazy.minimall.stock.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存盘点任务状态枚举，定义盘点任务生命周期状态
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum StockCountTaskStatusEnum implements BaseEnum {
    PENDING(1, "PENDING", "待盘点"),
    COMPLETED(2, "COMPLETED", "已完成"),
    CANCELLED(3, "CANCELLED", "已取消");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * Get the integer status code for persistence.
     * @return the integer code value
     */
    public int intCode() {
        return code;
    }
}
