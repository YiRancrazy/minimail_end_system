package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDto;
import com.yirancrazy.minimall.stock.entity.StockPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装库存聚合查询和基于条件 SQL 的原子库存变更。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface StockManager extends IService<StockPo> {

    /**
     * 汇总全部 SKU 数量、可用量、预占量和触发告警的 SKU 数量。
     *
     * @return 库存聚合结果
     */
    StockStatisticsDto statistics();

    /**
     * 在可用量充足时原子扣减可用量并等量增加预占量。
     *
     * @param skuId SKU ID
     * @param quantity 需要预占的数量
     * @return 实际更新行数；0 表示 SKU 不存在或可用量不足
     */
    int deductAvailable(Long skuId, long quantity);

    /**
     * 在预占量充足时原子减少预占量并等量恢复可用量。
     *
     * @param skuId SKU ID
     * @param quantity 需要释放的数量
     * @return 实际更新行数；0 表示 SKU 不存在或预占量不足
     */
    int restoreReserved(Long skuId, long quantity);

    /**
     * 原子增加指定 SKU 的可用量，不改变预占量。
     *
     * @param skuId SKU ID
     * @param quantity 需要增加的数量
     * @return 实际更新行数；0 表示 SKU 不存在
     */
    int increaseAvailable(Long skuId, long quantity);
}
