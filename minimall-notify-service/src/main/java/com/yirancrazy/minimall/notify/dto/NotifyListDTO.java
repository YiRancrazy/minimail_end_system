package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 站内信分页查询入参，支持按接收方/消息类型/已读状态过滤
 * @Version: 1.2
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyListDTO extends CursorPageDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    /** 接收方类型，见 RecipientTypeEnum */
    private Integer recipientType;

    /** 消息类型过滤，见 NotifyMessageTypeEnum；null 不过滤 */
    private Integer messageType;

    /** 已读过滤 0=未读 1=已读；null 不过滤 */
    private Integer readFlag;
}
