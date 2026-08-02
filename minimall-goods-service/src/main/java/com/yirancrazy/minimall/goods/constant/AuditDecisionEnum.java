package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品审核决策枚举，1=通过 2=驳回
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Getter
@AllArgsConstructor
public enum AuditDecisionEnum {
    APPROVE(1, "APPROVE", "通过"),
    REJECT(2, "REJECT", "驳回");

    private final int code;
    private final String alias;
    private final String message;
}
