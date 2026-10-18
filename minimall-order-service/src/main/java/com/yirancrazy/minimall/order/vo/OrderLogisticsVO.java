package com.yirancrazy.minimall.order.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderLogisticsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogisticsVO {
    private String node;
    private String description;
    private LocalDateTime createTime;
}
