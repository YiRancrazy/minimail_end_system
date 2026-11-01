package com.yirancrazy.minimall.notify.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载站内通知游标分页条件及用户、接收方类型、消息类型和已读状态筛选条件.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyListDto extends CursorPageDto {

    private Long userId;

    private Integer recipientType;

    private Integer messageType;

    private Integer readFlag;
}
