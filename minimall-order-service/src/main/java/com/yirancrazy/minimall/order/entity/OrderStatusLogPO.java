package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order_status_log，记录订单每次状态迁移的来源、目标和触发者。
 *   业务边界：orderVersion 作为同一订单版本的幂等去重依据，note 保存退款拒绝等业务说明。
 *   使用场景：状态机事务和商家退款列表回显使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_status_log")
public class OrderStatusLogPo extends BasePo {
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private Integer orderVersion;
    private String triggerSource;
    private Long operatorId;
    private String note;
}
