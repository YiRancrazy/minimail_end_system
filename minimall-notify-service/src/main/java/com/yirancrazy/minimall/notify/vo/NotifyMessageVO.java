package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMessageVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class NotifyMessageVO {

    private Long id;
    private Long userId;
    private Integer recipientType;

    private String type;
    private String title;
    private String content;

    private String status;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
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
     * 处理typeAlias。
     * @param code 编码
     * @return 处理结果
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
