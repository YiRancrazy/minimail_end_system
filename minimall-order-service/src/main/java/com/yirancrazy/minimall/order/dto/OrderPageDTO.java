package com.yirancrazy.minimall.order.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单游标分页和导出筛选条件。
 *   业务边界：支持用户、商家、状态、开始时间和结束时间过滤；cursor 与 limit 控制 ID 倒序游标分页。
 *   使用场景：用户、商家、平台分页及 CSV 导出共用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageDto extends CursorPageDto {

  private Long userId;
  private Long merchantId;

  private String status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
