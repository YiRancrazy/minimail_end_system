package com.yirancrazy.minimall.stock.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDTO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class StockManagerImpl extends ServiceImpl<StockMapper, StockPO>
    implements StockManager {

    /**
     * 执行 statistics 方法。
     * @return 处理结果
     */
    @Override
    public StockStatisticsDTO statistics() {
        return baseMapper.statistics();
    }

    /**
     * 执行 deductAvailable 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    @Override
    public int deductAvailable(Long skuId, long quantity) {
        return baseMapper.deductAvailable(skuId, quantity);
    }

    /**
     * 执行 restoreReserved 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    @Override
    public int restoreReserved(Long skuId, long quantity) {
        return baseMapper.restoreReserved(skuId, quantity);
    }

    /**
     * 执行 increaseAvailable 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    @Override
    public int increaseAvailable(Long skuId, long quantity) {
        return baseMapper.increaseAvailable(skuId, quantity);
    }
}
