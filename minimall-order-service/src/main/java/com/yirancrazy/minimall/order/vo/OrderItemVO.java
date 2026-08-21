package com.yirancrazy.minimall.order.vo;

import lombok.Data;
import com.yirancrazy.minimall.order.entity.OrderItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单商品明细VO，用于订单详情页展示商品名称、图片、单价与数量。
 * @Version: 1.0
 * @DateTime: 2026/08/20
 **/
@Data
public class OrderItemVO {

    private Long spuId;
    private Long skuId;
    /** 商品名称 */
    private String skuName;
    /** 商品主图地址 */
    private String skuImageUrl;
    private Integer quantity;
    /** 商品单价（元），字符串避免前端浮点精度问题 */
    private String unitPrice;
    /** 商品行小计（元），字符串 */
    private String amount;

    /**
     * 将订单明细持久化对象转换为 VO，金额一律转换为字符串避免浮点精度问题。
     * @param po 订单明细持久化对象
     * @return 订单明细VO
     */
    public static OrderItemVO from(OrderItemPO po) {
        OrderItemVO vo = new OrderItemVO();
        vo.setSpuId(po.getSpuId());
        vo.setSkuId(po.getSkuId());
        vo.setSkuName(po.getSkuName());
        vo.setSkuImageUrl(po.getSkuImageUrl());
        vo.setQuantity(po.getQuantity());
        vo.setUnitPrice(po.getUnitPrice() == null ? null : po.getUnitPrice().toPlainString());
        vo.setAmount(po.getAmount() == null ? null : po.getAmount().toPlainString());
        return vo;
    }
}