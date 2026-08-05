package com.yirancrazy.minimall.order.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单游标分页查询入参，支持按用户/商家/状态/时间范围过滤
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageDTO extends CursorPageDTO {

    private Long userId;
    private Long merchantId;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
