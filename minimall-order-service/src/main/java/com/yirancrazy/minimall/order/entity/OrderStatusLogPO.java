package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusLog持久化对象，映射t_order_status_log表，记录订单状态机流转日志。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_status_log")
public class OrderStatusLogPO extends BasePO {
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private String triggerSource;
    private Long operatorId;
    private String note;
}
