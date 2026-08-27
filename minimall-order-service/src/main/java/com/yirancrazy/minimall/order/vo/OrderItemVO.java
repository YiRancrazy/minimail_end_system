package com.yirancrazy.minimall.order.vo;

import lombok.Data;
import com.yirancrazy.minimall.common.util.MinioUtil;
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
        return from(po, null);
    }

    /**
     * 与单参版本一致，额外将商品图 skuImageUrl 的 objectKey 解析为可访问 URL（minioUtil 为 null 时原样返回）。
     * 落库的 skuImageUrl 为快照 objectKey，须在出参边界实时转预签名，避免过期 URL 被持久化。
     * @param po 订单明细持久化对象
     * @param minioUtil 对象存储工具，用于生成商品图预签名 URL
     * @return 订单明细VO
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