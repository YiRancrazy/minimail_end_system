package com.yirancrazy.minimall.order.manager.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDTO;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDTO;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDTO;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class OrderManagerImpl extends ServiceImpl<OrderMapper, OrderPO>
    implements OrderManager {


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Override
    public OrderStatisticsResultDTO statistics(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.statistics(merchantId, startTime, endTime);
    }


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    @Override
    public List<OrderStatusCountDTO> countByStatus(Long userId) {
        return baseMapper.countByStatus(userId);
    }


    /**
     * 执行 summary 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Override
    public OrderSummaryResultDTO summary(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.summary(merchantId, startTime, endTime);
    }


    /**
     * 执行 trend 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 结果列表
     */
    @Override
    public List<OrderTrendPointDTO> trend(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.trend(merchantId, startTime, endTime);
    }
}
