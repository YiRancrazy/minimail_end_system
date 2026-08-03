package com.yirancrazy.minimall.stock.mapper;

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
}
