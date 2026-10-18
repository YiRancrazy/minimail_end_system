package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderItemPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItemPO extends BasePO {
    private Long orderId;
    private Long spuId;
    private Long merchantId;
    private String spuSnapshotJson;
    private String skuSnapshotJson;
    private String skuImageUrl;
    private Long skuId;
    private String skuName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer refundStatus;
}
