package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质审核状态枚举。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum MerchantAuditStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待审核"),
    APPROVED(1, "APPROVED", "已通过"),
    REJECTED(2, "REJECTED", "已驳回");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
