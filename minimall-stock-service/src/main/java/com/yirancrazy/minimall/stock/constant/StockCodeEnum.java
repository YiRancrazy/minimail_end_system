package com.yirancrazy.minimall.stock.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
* 库存服务错误码枚举，实现 BaseEnum 并按 (code, alias, message) 三元组定义，
 *               覆盖 SKU 库存不存在 STOCK_NOT_FOUND 与库存不足 STOCK_INSUFFICIENT，
 *               供 BizException 抛出并由全局异常处理转为 Result.fail。
 */
@Getter
@AllArgsConstructor
public enum StockCodeEnum implements BaseEnum {
    STOCK_NOT_FOUND("18001", "STOCK_NOT_FOUND", "SKU 库存不存在"),
    STOCK_INSUFFICIENT("18002", "STOCK_INSUFFICIENT", "库存不足");

    private final String code;
    private final String alias;
    private final String message;
}