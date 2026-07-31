package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournal持久化对象，映射t_stock_journal表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock_journal")
public class StockJournalPO extends BasePO {
    private Long skuId;
    private Long quantity;
    private Integer type;
    private String reason;
    private String orderNo;
}
