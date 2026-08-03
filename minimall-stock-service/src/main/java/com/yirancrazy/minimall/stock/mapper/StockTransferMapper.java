package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransfer Mapper接口，提供t_stock_transfer表的基础CRUD操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface StockTransferMapper extends BaseMapper<StockTransferPO> {
}
