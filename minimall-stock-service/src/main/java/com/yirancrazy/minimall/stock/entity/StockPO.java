package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock")
public class StockPO extends BasePO {
    private Long skuId;
    private Long available;
    private Long reserved;
}