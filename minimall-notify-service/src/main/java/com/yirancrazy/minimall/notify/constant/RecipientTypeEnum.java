package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知接收方类型枚举，区分用户/商家/平台端站内信
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum RecipientTypeEnum implements BaseEnum {
    USER(1, "USER", "用户"),
    MERCHANT(2, "MERCHANT", "商家"),
    PLATFORM(3, "PLATFORM", "平台"),
    ALL(4, "ALL", "全部");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 获取用于持久化的整数编码。
     * @return 整数编码值
     */
    public int intCode() {
        return code;
    }
}
