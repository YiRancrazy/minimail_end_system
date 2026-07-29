package com.yirancrazy.minimall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SKU 持久化实体，对应商品 SKU 表，记录名称、价格、库存等核心售卖信息。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sku")
public class SkuPO extends BasePO {
    private Long spuId;
    private String skuName;
    private java.math.BigDecimal price;
    private Integer stock;
}