package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTask Mapper接口，提供t_stock_count_task表的基础CRUD操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface StockCountTaskMapper extends BaseMapper<StockCountTaskPO> {
}
