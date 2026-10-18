package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusLogPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
