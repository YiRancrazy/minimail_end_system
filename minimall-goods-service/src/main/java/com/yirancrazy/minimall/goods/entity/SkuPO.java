package com.yirancrazy.minimall.goods.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sku")
public class SkuPO extends BasePO {
    private Long spuId;
    // 归属商家ID：冗余自父SPU，用于商家侧越权校验
    private Long merchantId;
    private String skuName;
    private BigDecimal price;
    // 库存展示缓存，权威库存由库存服务的 t_stock.available 维护
    private Integer stock;
}
