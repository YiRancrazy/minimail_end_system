package com.yirancrazy.minimall.stock.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存流水类型枚举，定义库存变动的类型（预占/释放/调整/调出/调入/纠正）。
 * @Version: 1.1
 * @DateTime: 2026/08/03
**/
@Getter
@AllArgsConstructor
public enum StockJournalTypeEnum implements BaseEnum {
    RESERVE(1, "RESERVE", "预占"),
    RELEASE(2, "RELEASE", "释放"),
    ADJUST(3, "ADJUST", "调整"),
    TRANSFER_OUT(4, "TRANSFER_OUT", "调出"),
    TRANSFER_IN(5, "TRANSFER_IN", "调入");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * Get the integer type code for persistence.
     * @return the integer code value
     */
    public int intCode() {
        return code;
    }
}
