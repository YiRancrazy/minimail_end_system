package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价状态枚举，code 持久化为 TINYINT
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Getter
@AllArgsConstructor
public enum CommentStatusEnum implements BaseEnum {
    NORMAL(1, "NORMAL", "正常"),
    HIDDEN(2, "HIDDEN", "已隐藏"),
    DELETED(3, "DELETED", "已删除");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 获取持久化整数码。
     * @return 整数码
     */
    public int intCode() {
        return code;
    }
}
