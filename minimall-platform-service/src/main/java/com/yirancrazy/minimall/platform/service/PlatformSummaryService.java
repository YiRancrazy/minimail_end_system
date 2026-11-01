package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台经营汇总业务契约，提供订单统计和订单、支付对账导出数据.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PlatformSummaryService {

    /**
     * 查询平台订单各业务状态的数量汇总.
     *
     * @return 订单状态数量；远程服务失败时返回全零汇总
     */
    PlatformOrderSummaryVo orderSummary();

    /**
     * 按日期范围查询订单导出明细.
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单明细；远程服务失败时返回空列表
     */
    List<OrderExportItemDto> exportOrders(String startDate, String endDate);

    /**
     * 按日期范围查询支付对账明细.
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 支付流水明细；远程服务失败时返回空列表
     */
    List<PayExportItemDto> exportReconciliation(String startDate, String endDate);
}
