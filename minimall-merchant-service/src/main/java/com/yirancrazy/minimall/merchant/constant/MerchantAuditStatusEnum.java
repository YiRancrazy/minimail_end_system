package com.yirancrazy.minimall.merchant.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质审核状态枚举；定义待审核、通过和驳回三种持久化 code，供资质提交、平台审核与管理端查询共享。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum MerchantAuditStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待审核"),
    APPROVED(1, "APPROVED", "已通过"),
    REJECTED(2, "REJECTED", "已驳回");

    private final int code;
    private final String alias;
    private final String message;

    /**
     * 将审核状态的整数 code 转换为 BaseEnum 约定的字符串形式。
     *
     * @return 0（待审核）、1（已通过）或 2（已驳回）的字符串值。
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
