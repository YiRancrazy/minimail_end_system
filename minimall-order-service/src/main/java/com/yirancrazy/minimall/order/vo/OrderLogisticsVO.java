package com.yirancrazy.minimall.order.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载用户可见的单个物流节点。
 *   业务边界：只包含节点名称、描述和创建时间，不暴露物流主键或订单归属字段。
 *   使用场景：用户物流查询接口返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogisticsVo {
  private String node;
  private String description;
  private LocalDateTime createTime;
}
