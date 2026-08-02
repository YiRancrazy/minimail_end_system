package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品Spu错误码枚举，定义SPU相关错误码（商品域11000-11999）。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Getter
@AllArgsConstructor
public enum SpuCodeEnum implements BaseEnum {
    SPU_NOT_FOUND("11001", "SPU_NOT_FOUND", "SPU 不存在"),
    SPU_STATUS_INVALID("11002", "SPU_STATUS_INVALID", "SPU 状态不允许该操作"),
    SPU_NOT_PENDING_AUDIT("11003", "SPU_NOT_PENDING_AUDIT", "SPU 非待审核状态");

    private final String code;
    private final String alias;
    private final String message;
}
