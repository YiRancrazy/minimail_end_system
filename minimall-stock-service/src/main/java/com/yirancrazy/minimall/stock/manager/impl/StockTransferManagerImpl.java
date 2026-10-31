package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockTransferPo;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.mapper.StockTransferMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 MyBatis-Plus 提供库存调拨记录的保存、查询和分页能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class StockTransferManagerImpl extends ServiceImpl<StockTransferMapper, StockTransferPo>
    implements StockTransferManager {}
