package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVo;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供订单服务内部调用入口。
 *   业务边界：负责路径参数绑定和 Result 包装，业务校验收口在 OrderService；当前内部路径不应直接暴露公网。
 *   使用场景：供支付回调、其他微服务及平台统计通过内部接口调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/order")
public class InternalOrderControllerV1 {

    private final OrderService orderService;

    /**
     * 创建内部订单控制器并注入订单服务。
     *
     * <p>控制器是无状态 Spring Bean，全部业务调用都转交 OrderService。
     *
     * @param orderService 订单业务服务
     */
    public InternalOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 查询内部订单状态码。
     *
     * <p>按订单 ID 直接读取状态，供可信内部调用使用。
     *
     * @param id 订单 ID
     * @return 包含状态码的统一响应。
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable Long id) {
        return Result.success(orderService.getStatus(id));
    }

    /**
     * 按订单引用查询归属商家。
     *
     * <p>ref 可以是订单号或数字订单 ID，订单不存在时数据为 null。
     *
     * @param ref 订单号或数字订单 ID
     * @return 商家 ID 的统一响应。
     */
    @GetMapping("/merchant/{ref}")
    public Result<Long> merchantId(@PathVariable String ref) {
        return Result.success(orderService.resolveMerchantId(ref));
    }

    /**
     * 查询订单支付快照。
     *
     * <p>返回支付金额、状态、用户、商家和支付流水号。
     *
     * @param ref 订单号或数字订单 ID
     * @return 支付快照的统一响应。
     */
    @GetMapping("/pay-snapshot/{ref}")
    public Result<OrderPaySnapshotDto> paySnapshot(@PathVariable String ref) {
        return Result.success(orderService.getPaySnapshot(ref));
    }

    /**
     * 查询用户对指定 SPU 的评价资格。
     *
     * <p>用户 ID、订单号和 SPU ID 由请求参数提供，资格规则委托订单服务。
     *
     * @param orderNo 业务订单号
     * @param userId 用户 ID，必须对应当前订单归属
     * @param spuId SPU ID
     * @return 评价资格统一响应。
     */
    @GetMapping("/comment-eligibility")
    public Result<OrderCommentEligibilityDto> commentEligibility(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("userId") Long userId,
            @RequestParam("spuId") Long spuId) {
        return Result.success(orderService.commentEligibility(orderNo, userId, spuId));
    }

    /**
     * 触发内部订单支付状态推进。
     *
     * <p>仅按订单 ID 调用 OrderService.pay，不接收客户端支付凭证。
     *
     * @param id 订单 ID
     * @return 空数据的成功响应。
     * @throws BizException 订单不存在或状态不允许支付。
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success(null);
    }

    /**
     * 按订单号触发支付状态推进。
     *
     * <p>用于支付回调按业务订单号推进状态。
     *
     * @param orderNo 业务订单号
     * @return 空数据的成功响应。
     * @throws BizException 订单不存在或状态不允许支付。
     */
    @PostMapping("/pay-by-order-no/{orderNo}")
    public Result<Void> payByOrderNo(@PathVariable String orderNo) {
        orderService.payByOrderNo(orderNo);
        return Result.success(null);
    }

    /**
     * 接收支付服务退款结果回调。
     *
     * <p>请求体校验后交由订单服务更新累计退款金额和订单状态。
     *
     * @param dto 支付退款回调数据
     * @return 空数据的成功响应。
     */
    @PostMapping("/refund-callback")
    public Result<Void> refundCallback(@Valid @RequestBody RefundCallbackDto dto) {
        orderService.handleRefundCallback(dto);
        return Result.success(null);
    }

    /**
     * 查询全平台订单统计。
     *
     * <p>内部入口不传筛选条件，返回订单总量、金额和分状态计数。
     *
     * @return 内部统计 DTO 的统一响应。
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsDto> statistics() {
        return Result.success(toStatisticsDto(orderService.statistics(new OrderPageDto())));
    }

    /**
     * 按可选日期导出平台订单列表。
     *
     * <p>查询结果转换为内部导出条目，日期非法时由服务层忽略边界。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @return 内部导出条目统一响应。
     */
    @GetMapping("/export-list")
    public Result<List<OrderExportItemDto>> exportList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(
                orderService.platformExportVoList(startDate, endDate).stream()
                        .map(this::toExportItem)
                        .toList());
    }

    /**
     * 把订单统计视图转换为内部统计 DTO。
     *
     * <p>逐字段复制总量、金额和分状态计数，不改变统计口径。
     *
     * @param vo 订单统计视图
     * @return 内部统计 DTO。
     */
    private OrderStatisticsDto toStatisticsDto(OrderStatisticsVo vo) {
        return new OrderStatisticsDto(
                vo.getTotalOrderCount(),
                vo.getTotalAmount(),
                vo.getRefundAmount(),
                vo.getPendingCount(),
                vo.getPaidCount(),
                vo.getShippedCount(),
                vo.getReceivedCount(),
                vo.getCancelledCount(),
                vo.getRefundingCount(),
                vo.getRefundedCount());
    }

    /**
     * 把导出视图转换为内部导出条目。
     *
     * <p>复制订单 ID、用户、商家、SKU、数量、金额、状态和创建时间。
     *
     * @param vo 订单导出视图
     * @return 内部导出条目。
     */
    private OrderExportItemDto toExportItem(OrderExportVo vo) {
        return new OrderExportItemDto(
                vo.getId(),
                vo.getUserId(),
                vo.getMerchantId(),
                vo.getSkuId(),
                vo.getQuantity(),
                vo.getAmount(),
                vo.getStatus(),
                vo.getCreateTime());
    }
}
