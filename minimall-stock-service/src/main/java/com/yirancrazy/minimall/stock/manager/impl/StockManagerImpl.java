package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存数据访问管理实现，基于 MyBatis-Plus ServiceImpl 绑定 StockMapper 与 StockPO，
 *               为上层提供库存记录的查询与更新等通用 CRUD 支撑。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
@Manager
public class StockManagerImpl extends ServiceImpl<StockMapper, StockPO>
    implements StockManager {
}