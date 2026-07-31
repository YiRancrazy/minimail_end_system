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
 */
public class StockPO extends BasePO {
    private Long skuId;
    private Long available;
    private Long reserved;
}