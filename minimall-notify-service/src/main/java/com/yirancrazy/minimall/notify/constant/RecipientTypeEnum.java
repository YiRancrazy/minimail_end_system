package com.yirancrazy.minimall.notify.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义站内通知的接收方类型，区分用户、商家、平台以及 userId 为 0 的全端公告占位消息.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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

    /**
     * 将接收方类型编码转换为 BaseEnum 约定的字符串形式.
     *
     * @return 接收方类型的十进制字符串编码
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 返回可直接用于数据库接收方类型字段比较的整数编码.
     *
     * @return 接收方类型整数编码
     */
    public int intCode() {
        return code;
    }
}
