package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Stock MyBatis Mapper 接口，映射Stock表
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
public interface StockMapper extends BaseMapper<StockPO> {

    /**
     * 全平台库存统计聚合，返回SKU总数、可用/预占总量及预警SKU数。
     * @return 库存统计VO（alertRatio 由 Service 层计算填充）
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) AS totalSkuCount, "
        + "COALESCE(SUM(available), 0) AS totalAvailable, "
        + "COALESCE(SUM(reserved), 0) AS totalReserved, "
        + "COALESCE(SUM(CASE WHEN alert_threshold IS NOT NULL "
        + "AND available <= alert_threshold THEN 1 END), 0) AS alertSkuCount "
        + "FROM t_stock")
    StockStatisticsVO statistics();

    /**
     * 原子预占：扣减可用并等额增加预占，仅当可用数量充足时命中。
     * 条件内嵌保证并发下不会超卖（MySQL 行锁 + 检查式 UPDATE）。
     * @param skuId SKU标识
     * @param qty 预占数量
     * @return 影响行数，0 表示库存不足或记录不存在
     */
    @Update("UPDATE t_stock SET available = available - #{qty}, "
        + "reserved = reserved + #{qty}, update_time = NOW() "
        + "WHERE sku_id = #{skuId} AND available >= #{qty}")
    int deductAvailable(@Param("skuId") Long skuId, @Param("qty") long qty);

    /**
     * 原子释放：扣减预占并等额回补可用，仅当预占数量充足时命中。
     * @param skuId SKU标识
     * @param qty 释放数量
     * @return 影响行数，0 表示预占不足或记录不存在
     */
    @Update("UPDATE t_stock SET available = available + #{qty}, "
        + "reserved = reserved - #{qty}, update_time = NOW() "
        + "WHERE sku_id = #{skuId} AND reserved >= #{qty}")
    int restoreReserved(@Param("skuId") Long skuId, @Param("qty") long qty);

    /**
     * 原子增加可用库存（调拨入目标），仅更新已存在的记录。
     * @param skuId SKU标识
     * @param qty 增加数量
     * @return 影响行数，0 表示记录不存在
     */
    @Update("UPDATE t_stock SET available = available + #{qty}, "
        + "update_time = NOW() WHERE sku_id = #{skuId}")
    int increaseAvailable(@Param("skuId") Long skuId, @Param("qty") long qty);
}
