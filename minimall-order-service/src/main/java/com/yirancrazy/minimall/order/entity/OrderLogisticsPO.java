package com.yirancrazy.minimall.order.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单物流节点持久化对象，记录订单发货后的物流轨迹节点。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_logistics")
public class OrderLogisticsPO extends BasePO {
    private Long orderId;
    private String node;
    private String description;
    private LocalDateTime createdTime;
}
