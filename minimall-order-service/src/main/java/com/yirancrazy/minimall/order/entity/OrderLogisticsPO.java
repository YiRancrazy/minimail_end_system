package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order_logistics，保存订单物流节点。
 *   业务边界：节点按创建时间展示，description 保存承运商、运单号或签收说明；不调用第三方物流查询。
 *   使用场景：商家发货和用户物流查询使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_logistics")
public class OrderLogisticsPo extends BasePo {
    private Long orderId;
    private String node;
    private String description;
    private LocalDateTime createdTime;
}
