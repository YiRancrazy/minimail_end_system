package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


@Getter

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuCodeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AllArgsConstructor
public enum SkuCodeEnum implements BaseEnum {
    SKU_NOT_FOUND("13001", "SKU_NOT_FOUND", "SKU 不存在"),
    STOCK_ADJUST_FAILED("13002", "STOCK_ADJUST_FAILED", "库存服务调整失败");

    private final String code;
    private final String alias;
    private final String message;
}
