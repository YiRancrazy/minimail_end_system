package com.yirancrazy.minimall.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台订单按业务状态汇总的数量视图.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOrderSummaryVo {

  private Long paid;
  private Long pending;
  private Long refunding;
  private Long shipped;
  private Long completed;
  private Long cancelled;
}
