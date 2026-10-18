package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.mapper.StockTransferMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransferManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class StockTransferManagerImpl extends ServiceImpl<StockTransferMapper, StockTransferPO>
    implements StockTransferManager {
}
