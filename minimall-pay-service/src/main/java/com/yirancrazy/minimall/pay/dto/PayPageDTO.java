package com.yirancrazy.minimall.pay.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付流水浒标分页查询DTO，支持按状态与时间范围过滤。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PayPageDTO extends CursorPageDTO {

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
