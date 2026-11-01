package com.yirancrazy.minimall.goods.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 SPU 生命周期、审核状态和店铺校验相关的业务错误码。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum SpuCodeEnum implements BaseEnum {
    SPU_NOT_FOUND("11001", "SPU_NOT_FOUND", "SPU 不存在"),
    SPU_STATUS_INVALID("11002", "SPU_STATUS_INVALID", "SPU 状态不允许该操作"),
    SPU_NOT_PENDING_AUDIT("11003", "SPU_NOT_PENDING_AUDIT", "SPU 非待审核状态"),
    SPU_NOT_ON_SALE("11004", "SPU_NOT_ON_SALE", "SPU 非在售状态"),
    SPU_SHOP_INVALID("11005", "SPU_SHOP_INVALID", "店铺不存在或未营业，无法发布商品");

    private final String code;
    private final String alias;
    private final String message;
}
