package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournal Mapper接口，提供t_stock_journal表的基础CRUD操作。
 * @Version: 1.0
 * @DateTime: 2026/07/31
 **/
@Mapper
public interface StockJournalMapper extends BaseMapper<StockJournalPO> {
}
