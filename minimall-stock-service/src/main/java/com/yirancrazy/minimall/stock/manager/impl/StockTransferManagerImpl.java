package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.mapper.StockTransferMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransfer数据访问层实现，封装t_stock_transfer表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Manager
public class StockTransferManagerImpl extends ServiceImpl<StockTransferMapper, StockTransferPO>
    implements StockTransferManager {
}
