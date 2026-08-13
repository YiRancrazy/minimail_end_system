package com.yirancrazy.minimall.goods.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Sku持久化对象，映射sku表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sku")
public class SkuPO extends BasePO {
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
}