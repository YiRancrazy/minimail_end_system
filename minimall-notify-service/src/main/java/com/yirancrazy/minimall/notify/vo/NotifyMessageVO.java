package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 站内信VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class NotifyMessageVO {

    private Long id;
    private Long userId;
    private Integer recipientType;
    private Integer messageType;
    private String title;
    private String content;
    private Integer readFlag;
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
        vo.setMessageType(po.getMessageType());
        vo.setTitle(po.getTitle());
        vo.setContent(po.getContent());
        vo.setReadFlag(po.getReadFlag());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
