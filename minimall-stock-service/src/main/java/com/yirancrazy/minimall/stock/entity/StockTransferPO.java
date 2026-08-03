package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransfer持久化对象，映射t_stock_transfer跨商家库存调拨表
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock_transfer")
public class StockTransferPO extends BasePO {
    private Long fromSkuId;
    private Long toSkuId;
    private Long quantity;
    private String reason;
    private Long operatorId;
}
