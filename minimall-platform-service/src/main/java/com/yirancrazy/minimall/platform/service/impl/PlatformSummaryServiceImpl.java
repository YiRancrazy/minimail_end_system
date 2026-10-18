package com.yirancrazy.minimall.platform.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import com.yirancrazy.minimall.platform.service.PlatformSummaryService;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformSummaryServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformSummaryServiceImpl implements PlatformSummaryService {

    private final PlatformRemoteGateway platformRemoteGateway;


    /**
     * 执行 orderSummary 方法。
     * @return 处理结果
     */
    @Override
    public PlatformOrderSummaryVO orderSummary() {
        Result<OrderStatisticsDTO> result = platformRemoteGateway.orderStatistics();
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode()) || result.getData() == null) {
            log.warn("订单统计服务不可用，汇总数据返回零值。result={}", result);
            return zeros();
        }
        OrderStatisticsDTO statistics = result.getData();
        return new PlatformOrderSummaryVO(
            nvl(statistics.getPaidCount()), nvl(statistics.getPendingCount()),
            nvl(statistics.getRefundingCount()), nvl(statistics.getShippedCount()),
            nvl(statistics.getReceivedCount()), nvl(statistics.getCancelledCount()));
    }


    /**
     * 执行 exportOrders 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    @Override
    public List<OrderExportItemDTO> exportOrders(String startDate, String endDate) {
        Result<List<OrderExportItemDTO>> result = platformRemoteGateway.exportOrders(startDate, endDate);
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode()) || result.getData() == null) {
            log.warn("订单导出数据不可用，导出空文件。result={}", result);
            return List.of();
        }
        return result.getData();
    }


    /**
     * 执行 exportReconciliation 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    @Override
    public List<PayExportItemDTO> exportReconciliation(String startDate, String endDate) {
        Result<List<PayExportItemDTO>> result = platformRemoteGateway.exportPayTransactions(startDate, endDate);
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode()) || result.getData() == null) {
            log.warn("支付对账数据不可用，导出空文件。result={}", result);
            return List.of();
        }
        return result.getData();
    }


    /**
     * 处理zeros。
     * @return 视图对象
     */
    private PlatformOrderSummaryVO zeros() {
        return new PlatformOrderSummaryVO(0L, 0L, 0L, 0L, 0L, 0L);
    }


    /**
     * 处理nvl。
     * @param value 值
     * @return 处理结果
     */
    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
