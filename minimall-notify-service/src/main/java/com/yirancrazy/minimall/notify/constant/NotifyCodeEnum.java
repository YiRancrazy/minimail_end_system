package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知Notify错误码枚举，定义通知相关错误码
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Getter
@AllArgsConstructor
public enum NotifyCodeEnum implements BaseEnum {
    NOTIFY_NOT_FOUND("19001", "NOTIFY_NOT_FOUND", "消息不存在"),
    NOTIFY_NO_PERMISSION("19002", "NOTIFY_NO_PERMISSION", "无权操作该消息"),
    BROADCAST_RECIPIENT_INVALID("19003", "BROADCAST_RECIPIENT_INVALID", "广播接收方类型非法"),
    NOTIFY_BATCH_IDS_EMPTY("19004", "NOTIFY_BATCH_IDS_EMPTY", "消息ID列表不能为空"),
    NOTIFY_BATCH_TOO_MANY("19005", "NOTIFY_BATCH_TOO_MANY", "单次最多删除100条消息"),
    COMPLAINT_STATUS_INVALID("19006", "COMPLAINT_STATUS_INVALID", "投诉状态流转非法"),
    COMPLAINT_NOT_FOUND("19007", "COMPLAINT_NOT_FOUND", "投诉不存在"),
    COMMENT_RATING_INVALID("19101", "COMMENT_RATING_INVALID", "评分必须在 1-5 之间"),
    COMMENT_CONTENT_EMPTY("19102", "COMMENT_CONTENT_EMPTY", "评价内容不能为空"),
    COMMENT_NOT_FOUND("19103", "COMMENT_NOT_FOUND", "评价不存在"),
    COMMENT_ALREADY_REPLIED("19104", "COMMENT_ALREADY_REPLIED", "商家已回复过该评价"),
    COMMENT_NO_PERMISSION("19105", "COMMENT_NO_PERMISSION", "无权操作该评价"),
    PREFERENCE_CATEGORY_INVALID("19201", "PREFERENCE_CATEGORY_INVALID", "通知类别非法"),
    PREFERENCE_CHANNEL_INVALID("19202", "PREFERENCE_CHANNEL_INVALID", "通知渠道非法"),
    NOTIFY_SSE_FORBIDDEN("19301", "NOTIFY_SSE_FORBIDDEN", "无权订阅该用户的通知"),
    NOTIFY_SSE_LIMIT("19302", "NOTIFY_SSE_LIMIT", "通知连接数已达上限");

    private final String code;
    private final String alias;
    private final String message;
}
