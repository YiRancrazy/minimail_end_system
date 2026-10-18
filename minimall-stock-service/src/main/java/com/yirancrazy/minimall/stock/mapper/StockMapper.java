package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDTO;
import com.yirancrazy.minimall.stock.entity.StockPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface StockMapper extends BaseMapper<StockPO> {


    /**
     * 执行 statistics 方法。
     * @return 处理结果
     */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) AS totalSkuCount, "
        + "COALESCE(SUM(available), 0) AS totalAvailable, "
        + "COALESCE(SUM(reserved), 0) AS totalReserved, "
        + "COALESCE(SUM(CASE WHEN alert_threshold IS NOT NULL "
        + "AND available <= alert_threshold THEN 1 END), 0) AS alertSkuCount "
        + "FROM t_stock")
    StockStatisticsDTO statistics();


    /**
     * 执行 deductAvailable 方法。
     * @param skuId skuId 参数
     * @param qty qty 参数
     * @return 业务数值
     */
    @Update("UPDATE t_stock SET available = available - #{qty}, "
        + "reserved = reserved + #{qty}, version = version + 1, update_time = NOW() "
        + "WHERE sku_id = #{skuId} AND available >= #{qty}")
    int deductAvailable(@Param("skuId") Long skuId, @Param("qty") long qty);


    /**
     * 执行 restoreReserved 方法。
     * @param skuId skuId 参数
     * @param qty qty 参数
     * @return 业务数值
     */
    @Update("UPDATE t_stock SET available = available + #{qty}, "
        + "reserved = reserved - #{qty}, version = version + 1, update_time = NOW() "
        + "WHERE sku_id = #{skuId} AND reserved >= #{qty}")
    int restoreReserved(@Param("skuId") Long skuId, @Param("qty") long qty);


    /**
     * 执行 increaseAvailable 方法。
     * @param skuId skuId 参数
     * @param qty qty 参数
     * @return 业务数值
     */
    @Update("UPDATE t_stock SET available = available + #{qty}, "
        + "version = version + 1, update_time = NOW() WHERE sku_id = #{skuId}")
    int increaseAvailable(@Param("skuId") Long skuId, @Param("qty") long qty);
}
