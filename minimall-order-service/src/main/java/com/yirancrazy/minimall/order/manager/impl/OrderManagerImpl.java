package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDto;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDto;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDto;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDto;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现 OrderManager，通过 MyBatis-Plus 访问OrderPo。
 *   业务边界：作为 @Manager Bean 只处理持久化细节，不承载跨服务事务和业务状态机。
 *   使用场景：由订单服务或补偿任务依赖 base 接口调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
@RequiredArgsConstructor
public class OrderManagerImpl extends ServiceImpl<OrderMapper, OrderPo> implements OrderManager {

    private final OrderMapper orderMapper;

    /**
     * 执行订单统计 SQL。
     *
     * <p>按商家和时间范围聚合单量、支付金额、退款金额及分状态计数。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param startTime 筛选开始时间，包含边界
     * @param endTime 筛选结束时间，包含边界
     * @return 订单统计数据库投影。
     */
    @Override
    public OrderStatisticsResultDto statistics(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.statistics(merchantId, startTime, endTime);
    }

    /**
     * 按用户统计各订单状态数量。
     *
     * <p>只统计未删除订单并按 status 分组。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 状态与数量的分组列表。
     */
    @Override
    public List<OrderStatusCountDto> countByStatus(Long userId) {
        return baseMapper.countByStatus(userId);
    }

    /**
     * 查询订单经营汇总。
     *
     * <p>起止日期和商家 ID 均可空，格式非法时由服务层降级。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param startTime 筛选开始时间，包含边界
     * @param endTime 筛选结束时间，包含边界
     * @return 订单汇总统一响应。
     */
    @Override
    public OrderSummaryResultDto summary(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.summary(merchantId, startTime, endTime);
    }

    /**
     * 查询订单趋势。
     *
     * <p>按日期聚合单量和已支付金额，无数据返回空点位。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param startTime 筛选开始时间，包含边界
     * @param endTime 筛选结束时间，包含边界
     * @return 订单趋势统一响应。
     */
    @Override
    public List<OrderTrendPointDto> trend(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.trend(merchantId, startTime, endTime);
    }

    /**
     * 按 ID 游标查询支付已过期或缺少过期时间的订单。
     *
     * <p>使用 payExpireAt<=now 或 payExpireAt 为空作为兜底条件，并限制状态和批量大小。
     *
     * @param status 订单状态筛选值或状态编码
     * @param lastId 上一批最后一条订单 ID，用于游标续扫
     * @param pageSize 单批查询数量
     * @return 符合条件的订单列表。
     */
    @Override
    public List<OrderPo> listExpiredPendingOrders(Integer status, Long lastId, Integer pageSize) {
        Wrapper<OrderPo> wrappers =
                new LambdaQueryWrapper<OrderPo>()
                        .eq(status != null, OrderPo::getStatus, status)
                        .and(
                                e ->
                                        e.isNull(OrderPo::getPayExpireAt)
                                                .or()
                                                .le(OrderPo::getPayExpireAt, LocalDateTime.now()))
                        .gt(lastId != null, OrderPo::getId, lastId)
                        .orderByAsc(OrderPo::getId)
                        .last(pageSize != null, "LIMIT " + pageSize);
        return orderMapper.selectList(wrappers);
    }
}
