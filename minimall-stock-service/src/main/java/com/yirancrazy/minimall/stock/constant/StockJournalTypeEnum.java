package com.yirancrazy.minimall.stock.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournalTypeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
public enum StockJournalTypeEnum implements BaseEnum {
    RESERVE(1, "RESERVE", "预占"),
    RELEASE(2, "RELEASE", "释放"),
    ADJUST(3, "ADJUST", "调整"),
    TRANSFER_OUT(4, "TRANSFER_OUT", "调出"),
    TRANSFER_IN(5, "TRANSFER_IN", "调入"),
    CORRECT(6, "CORRECT", "纠正");

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
