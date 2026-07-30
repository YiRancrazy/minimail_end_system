package com.yirancrazy.minimall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockPO;

/**
* 库存表 t_stock 的 MyBatis-Plus Mapper，继承 BaseMapper 提供 StockPO 的
 *               单表增删改查能力，由 StockManager 统一调用。
 */
public interface StockMapper extends BaseMapper<StockPO> {
}