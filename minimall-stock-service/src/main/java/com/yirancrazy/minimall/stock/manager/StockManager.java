package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDTO;
import com.yirancrazy.minimall.stock.entity.StockPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface StockManager extends IService<StockPO> {

    /**
     * 执行 statistics 方法。
     * @return 处理结果
     */
    StockStatisticsDTO statistics();

    /**
     * 执行 deductAvailable 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    int deductAvailable(Long skuId, long quantity);

    /**
     * 执行 restoreReserved 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    int restoreReserved(Long skuId, long quantity);

    /**
     * 执行 increaseAvailable 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    int increaseAvailable(Long skuId, long quantity);
}
