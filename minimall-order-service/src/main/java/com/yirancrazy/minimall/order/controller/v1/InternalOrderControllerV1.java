package com.yirancrazy.minimall.order.controller.v1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单内部控制器，提供Order相关内部接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@RestController
@RequestMapping("/internal/order")
public class InternalOrderControllerV1 {

    private final OrderService orderService;

    public InternalOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询指定订单的当前业务状态。
     *
     * @param id 订单标识
     * @return 订单状态；订单不存在时返回 UNKNOWN
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable Long id) {
        return Result.success(orderService.getStatus(id));
    }

    /**
     * 按订单标识（订单ID或业务单号）解析订单归属商户ID，供支付服务归属收款商户。
     *
     * @param ref 订单标识
     * @return 商户ID；订单不存在时返回 null
     */
    @GetMapping("/merchant/{ref}")
    public Result<Long> merchantId(@PathVariable String ref) {
        return Result.success(orderService.resolveMerchantId(ref));
    }

    /**
     * 推进指定订单为已支付状态，并广播 OrderPaidDTO 事件。
     *
     * @param id 订单标识
     * @return 空响应体
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success(null);
    }

    /**
     * 按业务单号推进订单为已支付状态，供支付回调（C 端携带业务单号）使用。
     *
     * @param orderNo 业务单号
     * @return 空响应体
     */
    @PostMapping("/pay-by-order-no/{orderNo}")
    public Result<Void> payByOrderNo(@PathVariable String orderNo) {
        orderService.payByOrderNo(orderNo);
        return Result.success(null);
    }

    /**
     * 接收支付服务退款结果回调，推进订单退款状态。
     *
     * @param id 订单标识
     * @param success 退款是否成功
     * @return 空响应体
     */
    @PostMapping("/refund-callback/{id}")
    public Result<Void> refundCallback(@PathVariable Long id, @RequestParam boolean success) {
        orderService.handleRefundCallback(id, success);
        return Result.success(null);
    }

    /**
     * 全平台订单统计聚合，供平台经营报表跨服务调用。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @return 订单统计DTO
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsDTO> statistics() {
        return Result.success(toStatisticsDto(orderService.statistics(new OrderPageDTO())));
    }

    /**
     * 全平台订单导出列表，按创建时间倒序，服务端限制最大导出行数。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param startDate 起始日期（yyyy-MM-dd），null 表示不限制
     * @param endDate 结束日期（yyyy-MM-dd），null 表示不限制
     * @return 订单导出项列表
     */
    @GetMapping("/export-list")
    public Result<List<OrderExportItemDTO>> exportList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        OrderPageDTO dto = new OrderPageDTO();
        dto.setStartTime(parseStartDate(startDate));
        dto.setEndTime(parseEndDate(endDate));
        return Result.success(orderService.platformExportList(dto).stream().map(this::toExportItem).toList());
    }

    /**
     * 起始日期 → 当日零点。
     * @param date 日期字符串（yyyy-MM-dd），空返回 null
     * @return 当日零点时间
     */
    private LocalDateTime parseStartDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date).atStartOfDay();
    }

    /**
     * 结束日期 → 当日最后一毫秒。
     * @param date 日期字符串（yyyy-MM-dd），空返回 null
     * @return 当日末刻时间
     */
    private LocalDateTime parseEndDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date).atTime(LocalTime.MAX);
    }

    /**
     * 统计VO → 跨服务DTO。
     * @param vo 订单统计VO
     * @return 订单统计DTO
     */
    private OrderStatisticsDTO toStatisticsDto(OrderStatisticsVO vo) {
        return new OrderStatisticsDTO(vo.getTotalOrderCount(), vo.getTotalAmount(), vo.getRefundAmount(),
            vo.getPendingCount(), vo.getPaidCount(), vo.getShippedCount(), vo.getReceivedCount(),
            vo.getCancelledCount(), vo.getRefundingCount(), vo.getRefundedCount());
    }

    /**
     * 订单PO → 导出项DTO。
     * @param po 订单持久化对象
     * @return 订单导出项DTO
     */
    private OrderExportItemDTO toExportItem(OrderPO po) {
        return new OrderExportItemDTO(po.getId(), po.getUserId(), po.getMerchantId(), po.getSkuId(),
            po.getQuantity(), po.getAmount(), po.getStatus(), po.getCreateTime());
    }
}