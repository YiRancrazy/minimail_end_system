package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_stock_count_task，保存盘点任务的账面量、实盘量和差异结果。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock_count_task")
public class StockCountTaskPo extends BasePo {
    private Long skuId;
    private Long expectedQuantity;
    private Long actualQuantity;
    private Long diffQuantity;
    private Integer status;
    private String remark;
    private Long operatorId;
}
