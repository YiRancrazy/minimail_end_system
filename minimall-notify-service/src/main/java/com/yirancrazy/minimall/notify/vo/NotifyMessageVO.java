package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 站内信VO，用于Controller边界输出，隐藏内部字段；status/type 输出枚举别名供前端直接消费
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
public class NotifyMessageVO {

    private Long id;
    private Long userId;
    private Integer recipientType;
    /** 消息类型枚举别名（如 ORDER/LOGISTICS/SYSTEM），未知码原样返回数字字符串 */
    private String type;
    private String title;
    private String content;
    /** 已读状态枚举别名：UNREAD / READ */
    private String status;
    private LocalDateTime createTime;

    /**
     * 将 NotifyMessagePO 转换为 NotifyMessageVO。
     * @param po 站内信持久化对象
     * @return 站内信VO
     */
    public static NotifyMessageVO from(NotifyMessagePO po) {
        NotifyMessageVO vo = new NotifyMessageVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setRecipientType(po.getRecipientType());
        vo.setType(typeAlias(po.getMessageType()));
        vo.setTitle(po.getTitle());
        vo.setContent(po.getContent());
        vo.setStatus(Integer.valueOf(1).equals(po.getReadFlag()) ? "READ" : "UNREAD");
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * 将持久化消息类型码映射为枚举别名，未知码原样返回数字字符串避免丢失类型。
     * @param code 持久化消息类型码
     * @return 消息类型枚举别名
     */
    private static String typeAlias(Integer code) {
        for (NotifyMessageTypeEnum e : NotifyMessageTypeEnum.values()) {
            if (e.getCode().equals(String.valueOf(code))) {
                return e.getAlias();
            }
        }
        return String.valueOf(code);
    }
}
