package com.yirancrazy.minimall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDto;
import com.yirancrazy.minimall.stock.entity.StockPo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 访问 t_stock，提供库存聚合和带库存下限条件的原子更新 SQL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface StockMapper extends BaseMapper<StockPo> {

    /**
     * 汇总全部 SKU 数量、可用量、预占量和触发告警的 SKU 数量。
     *
     * @return 库存聚合结果；空表时数值聚合列由 SQL 的 COALESCE 返回 0
     */
    @org.apache.ibatis.annotations.Select(
            "SELECT COUNT(*) AS totalSkuCount, "
                    + "COALESCE(SUM(available), 0) AS totalAvailable, "
                    + "COALESCE(SUM(reserved), 0) AS totalReserved, "
                    + "COALESCE(SUM(CASE WHEN alert_threshold IS NOT NULL "
                    + "AND available <= alert_threshold THEN 1 END), 0) AS alertSkuCount "
                    + "FROM t_stock")
    StockStatisticsDto statistics();

    /**
     * 在可用量不小于扣减量时减少可用量、增加预占量并递增版本号。
     *
     * @param skuId SKU ID
     * @param qty 预占数量
     * @return 实际更新行数；0 表示 SKU 不存在或可用量不足
     */
    @Update(
            "UPDATE t_stock SET available = available - #{qty}, "
                    + "reserved = reserved + #{qty}, version = version + 1, update_time = NOW() "
                    + "WHERE sku_id = #{skuId} AND available >= #{qty}")
    int deductAvailable(@Param("skuId") Long skuId, @Param("qty") long qty);

    /**
     * 在预占量不小于释放量时减少预占量、恢复可用量并递增版本号。
     *
     * @param skuId SKU ID
     * @param qty 释放数量
     * @return 实际更新行数；0 表示 SKU 不存在或预占量不足
     */
    @Update(
            "UPDATE t_stock SET available = available + #{qty}, "
                    + "reserved = reserved - #{qty}, version = version + 1, update_time = NOW() "
                    + "WHERE sku_id = #{skuId} AND reserved >= #{qty}")
    int restoreReserved(@Param("skuId") Long skuId, @Param("qty") long qty);

    /**
     * 增加指定 SKU 的可用量并递增版本号，不校验库存上限。
     *
     * @param skuId SKU ID
     * @param qty 增加数量
     * @return 实际更新行数；0 表示 SKU 不存在
     */
    @Update(
            "UPDATE t_stock SET available = available + #{qty}, "
                    + "version = version + 1, update_time = NOW() WHERE sku_id = #{skuId}")
    int increaseAvailable(@Param("skuId") Long skuId, @Param("qty") long qty);
}
