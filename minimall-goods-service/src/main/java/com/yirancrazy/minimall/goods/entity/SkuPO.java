package com.yirancrazy.minimall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sku")
public class SkuPO extends BasePO {
    private Long spuId;
    private String skuName;
    private java.math.BigDecimal price;
    private Integer stock;
}