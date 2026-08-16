package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Stock持久化对象，映射stock表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock")
public class StockPO extends BasePO {
    private Long skuId;

    /**
     * 库存归属商家ID，商家端越权校验依据；null 表示历史无归属记录（不参与商家端操作）
     */
    private Long merchantId;
    private Long available;
    private Long reserved;
    private Long alertThreshold;
}