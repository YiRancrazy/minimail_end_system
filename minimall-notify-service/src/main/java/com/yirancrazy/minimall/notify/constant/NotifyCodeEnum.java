package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知Notify错误码枚举，定义通知相关错误码
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Getter
@AllArgsConstructor
public enum NotifyCodeEnum implements BaseEnum {
    NOTIFY_NOT_FOUND("19001", "NOTIFY_NOT_FOUND", "消息不存在"),
    NOTIFY_NO_PERMISSION("19002", "NOTIFY_NO_PERMISSION", "无权操作该消息"),
    BROADCAST_RECIPIENT_INVALID("19003", "BROADCAST_RECIPIENT_INVALID", "广播接收方类型非法");

    private final String code;
    private final String alias;
    private final String message;
}
