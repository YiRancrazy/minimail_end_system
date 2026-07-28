package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;
import org.springframework.stereotype.Service;

@Service
@Manager
public class StockManagerImpl extends ServiceImpl<StockMapper, StockPO>
    implements StockManager {
}