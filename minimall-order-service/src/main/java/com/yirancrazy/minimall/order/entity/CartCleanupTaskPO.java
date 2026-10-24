package com.yirancrazy.minimall.order.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算后的购物车清理任务。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_cart_cleanup_task")
public class CartCleanupTaskPO extends BasePO {

    private String orderGroupNo;

    private Long userId;

    private String itemsJson;

    private Integer status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String lastError;
}
