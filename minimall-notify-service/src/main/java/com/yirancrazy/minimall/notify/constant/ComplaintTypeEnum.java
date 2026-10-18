package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintTypeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
