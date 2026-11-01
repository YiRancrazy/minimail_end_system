package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order_cart_cleanup_task，保存结算后待清理购物车条目的异步任务。
 *   业务边界：itemsJson 保存条目快照，status、retryCount、nextRetryAt 和 lastError 支撑重试；不保存订单明细。
 *   使用场景：OrderServiceImpl 创建任务，CartCleanupServiceImpl 领取并执行清理。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_cart_cleanup_task")
public class CartCleanupTaskPo extends BasePo {

    private String orderGroupNo;

    private Long userId;

    private String itemsJson;

    private Integer status;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String lastError;
}
