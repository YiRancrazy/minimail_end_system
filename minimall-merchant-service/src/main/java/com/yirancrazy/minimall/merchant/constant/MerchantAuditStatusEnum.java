package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantAuditStatusEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
