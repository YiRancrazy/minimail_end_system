package com.yirancrazy.minimall.notify.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载投诉游标分页条件及状态、订单号、投诉人、被投诉人筛选条件；用户端会强制覆盖投诉人编号.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ComplaintPageDto extends CursorPageDto {

    private Integer status;

    private String orderNo;

    private Long userId;

    private Long defendantId;
}
