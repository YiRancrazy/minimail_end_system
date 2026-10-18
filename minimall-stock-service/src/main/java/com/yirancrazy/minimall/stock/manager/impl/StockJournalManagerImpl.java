package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.mapper.StockJournalMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournalManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class StockJournalManagerImpl extends ServiceImpl<StockJournalMapper, StockJournalPO>
    implements StockJournalManager {
}
