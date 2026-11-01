package com.yirancrazy.minimall.pay.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载支付流水的游标分页及筛选条件。
 * 支持状态、创建时间范围和游标限制，供商家与平台查询和 CSV 导出复用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayPageDto extends CursorPageDto {

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
