package com.yirancrazy.minimall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockTransferPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 MyBatis-Plus 访问 t_stock_transfer 库存调拨审计表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface StockTransferMapper extends BaseMapper<StockTransferPo> {}
