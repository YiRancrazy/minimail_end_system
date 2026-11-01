package com.yirancrazy.minimall.notify.vo;

import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 站内通知对外视图，将消息类型编码和已读标记转换为前端可直接使用的类型别名与 READ/UNREAD 状态.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class NotifyMessageVo {

    private Long id;
    private Long userId;
    private Integer recipientType;

    private String type;
    private String title;
    private String content;

    private String status;
    private LocalDateTime createTime;

    /**
     * 将通知实体转换为视图，并把消息类型编码解析为枚举别名、readFlag 解析为 READ 或 UNREAD.
     *
     * @param po 通知实体
     * @return 通知视图
     */
    public static NotifyMessageVo from(NotifyMessagePo po) {
        NotifyMessageVo vo = new NotifyMessageVo();
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
     * 查找消息类型编码对应的枚举别名；未知编码返回编码字符串本身.
     *
     * @param code 消息类型整数编码，可为 null
     * @return 枚举别名或原编码字符串
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
