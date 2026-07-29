package com.yirancrazy.minimall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存表 t_stock 的 MyBatis-Plus Mapper，继承 BaseMapper 提供 StockPO 的
 *               单表增删改查能力，由 StockManager 统一调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface StockMapper extends BaseMapper<StockPO> {
}