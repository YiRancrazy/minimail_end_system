package com.yirancrazy.minimall.stock.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存Stock错误码枚举，定义库存相关错误码
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Getter
@AllArgsConstructor
public enum StockCodeEnum implements BaseEnum {
    STOCK_NOT_FOUND("18001", "STOCK_NOT_FOUND", "SKU 库存不存在"),
    STOCK_INSUFFICIENT("18002", "STOCK_INSUFFICIENT", "库存不足"),
    THRESHOLD_INVALID("60001", "THRESHOLD_INVALID", "Alert threshold must be >= 0"),
    ADJUST_QUANTITY_ZERO("60002", "ADJUST_QUANTITY_ZERO", "Adjust quantity cannot be zero"),
    STOCK_TRANSFER_SAME_SKU("60003", "STOCK_TRANSFER_SAME_SKU", "调出与调入SKU不能相同"),
    STOCK_TRANSFER_INSUFFICIENT("60004", "STOCK_TRANSFER_INSUFFICIENT", "源库存不足"),
    STOCK_TRANSFER_TARGET_NOT_FOUND("60005", "STOCK_TRANSFER_TARGET_NOT_FOUND", "目标SKU库存记录不存在"),
    STOCK_COUNT_TASK_NOT_FOUND("60006", "STOCK_COUNT_TASK_NOT_FOUND", "盘点任务不存在"),
    STOCK_COUNT_TASK_NOT_PENDING("60007", "STOCK_COUNT_TASK_NOT_PENDING", "盘点任务非待盘点状态"),
    STOCK_COUNT_TASK_ACTUAL_INVALID("60008", "STOCK_COUNT_TASK_ACTUAL_INVALID", "实际盘点数量不能为负");

    private final String code;
    private final String alias;
    private final String message;
}