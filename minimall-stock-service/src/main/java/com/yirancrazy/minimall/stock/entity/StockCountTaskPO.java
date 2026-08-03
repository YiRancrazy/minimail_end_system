package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTask持久化对象，映射t_stock_count_task库存盘点任务表
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock_count_task")
public class StockCountTaskPO extends BasePO {
    private Long skuId;
    private Long expectedQuantity;
    private Long actualQuantity;
    private Long diffQuantity;
    private Integer status;
    private String remark;
    private Long operatorId;
}
