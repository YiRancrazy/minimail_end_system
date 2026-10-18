package com.yirancrazy.minimall.pay.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayPageDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PayPageDTO extends CursorPageDTO {

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
