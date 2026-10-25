package com.yirancrazy.minimall.order.manager.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OrderManagerImpl extends ServiceImpl<OrderMapper, OrderPO>
    implements OrderManager {

    private final OrderMapper orderMapper;


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

    /**
     * 列出过期的待处理订单。
     * @param status 订单状态
     * @param lastId 最后一个订单ID
     * @param pageSize 每页大小
     * @return 订单列表
     */
    @Override
    public List<OrderPO> listExpiredPendingOrders(Integer status, Long lastId, Integer pageSize) {
        Wrapper<OrderPO> wrappers = new LambdaQueryWrapper<OrderPO>()
                .eq(status != null, OrderPO::getStatus, status)
                .and(e -> e.isNull(OrderPO::getPayExpireAt)
                        .or()
                        .le(OrderPO::getPayExpireAt, LocalDateTime.now()))
                .gt(lastId != null, OrderPO::getId, lastId)
                .orderByAsc(OrderPO::getId)
                .last(pageSize != null, "LIMIT " + pageSize);
        return orderMapper.selectList(wrappers);
    }
}
