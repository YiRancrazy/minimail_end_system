package com.yirancrazy.minimall.order.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单物流节点视图对象，用于返回物流轨迹查询结果。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogisticsVO {
    private String node;
    private String description;
    private LocalDateTime createTime;
}
