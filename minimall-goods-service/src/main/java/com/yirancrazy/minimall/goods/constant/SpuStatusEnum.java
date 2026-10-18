package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuStatusEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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
     * 执行 statusValue 方法。
     * @return 业务数值
     */
    public int statusValue() {
        return Integer.parseInt(code);
    }
}
