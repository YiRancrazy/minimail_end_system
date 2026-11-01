package com.yirancrazy.minimall.id.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部 ID 请求的业务标签目录。每个枚举值通过 BaseEnum 暴露 code、alias
 *     和展示消息；当前 Snowflake 生成器不会根据这些标签分支。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum IdBizTagEnum implements BaseEnum {
    ORDER("order", "ORDER_ID", "订单"),
    PAY("pay", "PAY_ID", "支付"),
    STOCK("stock", "STOCK_ID", "库存"),
    USER("user", "USER_ID", "用户");

    private final String code;
    private final String alias;
    private final String message;
}
