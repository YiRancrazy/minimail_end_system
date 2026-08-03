package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 站内信分页查询入参，支持按接收方/消息类型/已读状态过滤
 * @Version: 1.1
 * @DateTime: 2026/08/03
 **/
@Data
public class NotifyListDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    @NotNull(message = "userId cannot be null")
    private Long userId;

    /** 接收方类型，见 RecipientTypeEnum */
    private Integer recipientType;

    /** 消息类型过滤，见 NotifyMessageTypeEnum；null 不过滤 */
    private Integer messageType;

    /** 已读过滤 0=未读 1=已读；null 不过滤 */
    private Integer readFlag;
}
