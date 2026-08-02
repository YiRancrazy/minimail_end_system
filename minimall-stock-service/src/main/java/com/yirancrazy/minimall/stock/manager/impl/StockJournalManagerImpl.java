package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.mapper.StockJournalMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournal数据访问层实现，封装t_stock_journal表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Manager
public class StockJournalManagerImpl extends ServiceImpl<StockJournalMapper, StockJournalPO>
    implements StockJournalManager {
}
