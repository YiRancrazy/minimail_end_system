package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品SPU状态枚举，定义SPU生命周期状态，code持久化为TINYINT。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Getter
@AllArgsConstructor
public enum SpuStatusEnum implements BaseEnum {
    DRAFT("0", "DRAFT", "草稿"),
    PENDING_AUDIT("1", "PENDING_AUDIT", "待审"),
    ON_SALE("2", "ON_SALE", "在售"),
    OFF_SHELF("3", "OFF_SHELF", "下架"),
    REJECTED("4", "REJECTED", "审核驳回");

    private final String code;
    private final String alias;
    private final String message;

    /**
     * 获取持久化用的整型状态值。
     * @return 状态码整数值
     */
    public int statusValue() {
        return Integer.parseInt(code);
    }
}
