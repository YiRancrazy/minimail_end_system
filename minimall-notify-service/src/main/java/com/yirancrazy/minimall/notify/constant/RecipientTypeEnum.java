package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RecipientTypeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }


    /**
     * 执行 intCode 方法。
     * @return 业务数值
     */
    public int intCode() {
        return code;
    }
}
