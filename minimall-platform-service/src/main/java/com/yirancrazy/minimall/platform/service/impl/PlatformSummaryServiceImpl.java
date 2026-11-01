package com.yirancrazy.minimall.platform.service.impl;

import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import com.yirancrazy.minimall.platform.service.PlatformSummaryService;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台经营汇总实现，通过远程网关读取订单统计与导出明细，并提供降级空结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformSummaryServiceImpl implements PlatformSummaryService {

    private final PlatformRemoteGateway platformRemoteGateway;

    /**
     * 查询订单状态统计并转换为平台汇总视图.
     *
     * <p>远程响应失败或数据为空时记录警告并返回全零汇总，空计数按 0 处理。
     *
     * @return 订单状态数量汇总
     */
    @Override
    public PlatformOrderSummaryVo orderSummary() {
        Result<OrderStatisticsDto> result = platformRemoteGateway.orderStatistics();
        if (result == null
                || !CommonCode.SUCCESS.equals(result.getCode())
                || result.getData() == null) {
            log.warn("订单统计服务不可用，汇总数据返回零值。result={}", result);
            return zeros();
        }
        OrderStatisticsDto statistics = result.getData();
        return new PlatformOrderSummaryVo(
                nvl(statistics.getPaidCount()), nvl(statistics.getPendingCount()),
                nvl(statistics.getRefundingCount()), nvl(statistics.getShippedCount()),
                nvl(statistics.getReceivedCount()), nvl(statistics.getCancelledCount()));
    }

    /**
     * 查询订单导出明细，并在远程失败时提供空列表.
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单导出明细或空列表
     */
    @Override
    public List<OrderExportItemDto> exportOrders(String startDate, String endDate) {
        Result<List<OrderExportItemDto>> result =
                platformRemoteGateway.exportOrders(startDate, endDate);
        if (result == null
                || !CommonCode.SUCCESS.equals(result.getCode())
                || result.getData() == null) {
            log.warn("订单导出数据不可用，导出空文件。result={}", result);
            return List.of();
        }
        return result.getData();
    }

    /**
     * 查询支付流水导出明细，并在远程失败时提供空列表.
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 支付对账明细或空列表
     */
    @Override
    public List<PayExportItemDto> exportReconciliation(String startDate, String endDate) {
        Result<List<PayExportItemDto>> result =
                platformRemoteGateway.exportPayTransactions(startDate, endDate);
        if (result == null
                || !CommonCode.SUCCESS.equals(result.getCode())
                || result.getData() == null) {
            log.warn("支付对账数据不可用，导出空文件。result={}", result);
            return List.of();
        }
        return result.getData();
    }

    /**
     * 创建所有订单状态计数均为零的汇总视图.
     *
     * @return 全零订单汇总
     */
    private PlatformOrderSummaryVo zeros() {
        return new PlatformOrderSummaryVo(0L, 0L, 0L, 0L, 0L, 0L);
    }

    /**
     * 将可能为空的远程计数转换为基本类型零值.
     *
     * @param value 远程计数
     * @return 非空计数；入参为 null 时返回 0
     */
    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
