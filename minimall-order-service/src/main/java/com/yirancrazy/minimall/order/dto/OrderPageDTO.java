package com.yirancrazy.minimall.order.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPageDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageDTO extends CursorPageDTO {

    private Long userId;
    private Long merchantId;

    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
