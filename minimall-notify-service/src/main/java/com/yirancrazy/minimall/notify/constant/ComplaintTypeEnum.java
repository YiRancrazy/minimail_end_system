package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉类型枚举，持久化为 VARCHAR(32)
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum ComplaintTypeEnum implements BaseEnum {
    QUALITY("QUALITY", "QUALITY", "质量问题"),
    SERVICE("SERVICE", "SERVICE", "服务问题"),
    FRAUD("FRAUD", "FRAUD", "欺诈行为"),
    OTHER("OTHER", "OTHER", "其他");

    private final String code;
    private final String alias;
    private final String message;
}
