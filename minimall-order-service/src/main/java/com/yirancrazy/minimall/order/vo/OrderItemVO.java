package com.yirancrazy.minimall.order.vo;

import lombok.Data;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.entity.OrderItemPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderItemVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderItemVO {

    private Long spuId;
    private Long skuId;

    private String spuName;

    private String skuName;

    private String skuImageUrl;
    private Integer quantity;

    private String unitPrice;

    private String amount;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static OrderItemVO from(OrderItemPO po) {
        return from(po, null);
    }


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @param minioUtil minioUtil 参数
     * @return 处理结果
     */
    public static OrderItemVO from(OrderItemPO po, MinioUtil minioUtil) {
        OrderItemVO vo = new OrderItemVO();
        vo.setSpuId(po.getSpuId());
        vo.setSkuId(po.getSkuId());
        vo.setSkuName(po.getSkuName());
        vo.setSkuImageUrl(minioUtil == null ? po.getSkuImageUrl() : minioUtil.resolvePublicUrl(po.getSkuImageUrl()));
        vo.setQuantity(po.getQuantity());
        vo.setUnitPrice(po.getUnitPrice() == null ? null : po.getUnitPrice().toPlainString());
        vo.setAmount(po.getAmount() == null ? null : po.getAmount().toPlainString());
        return vo;
    }
}