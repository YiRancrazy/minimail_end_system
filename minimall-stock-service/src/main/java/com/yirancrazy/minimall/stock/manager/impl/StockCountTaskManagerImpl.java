package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.mapper.StockCountTaskMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTask数据访问层实现，封装t_stock_count_task表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Manager
public class StockCountTaskManagerImpl extends ServiceImpl<StockCountTaskMapper, StockCountTaskPO>
    implements StockCountTaskManager {
}
