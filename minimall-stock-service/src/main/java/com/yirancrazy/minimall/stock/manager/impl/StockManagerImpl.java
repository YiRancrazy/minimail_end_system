package com.yirancrazy.minimall.stock.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Stock数据访问层实现，封装Stock表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class StockManagerImpl extends ServiceImpl<StockMapper, StockPO>
    implements StockManager {
}