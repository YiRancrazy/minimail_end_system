package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
* 库存持久化实体，对应库存表 t_stock，以 SKU 为维度记录可用数量 available
 *               与已预占数量 reserved；公共主键与审计字段继承自 BasePO。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock")
public class StockPO extends BasePO {
    private Long skuId;
    private Long available;
    private Long reserved;
}