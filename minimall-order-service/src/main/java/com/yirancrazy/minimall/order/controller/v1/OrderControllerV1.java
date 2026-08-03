package com.yirancrazy.minimall.order.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.order.dto.OrderCreateDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单控制器，提供Order RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {

    private final OrderService orderService;

    public OrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 分页查询订单列表，支持按用户/商家/状态过滤。
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    @GetMapping
    public Result<IPage<OrderPO>> page(@Valid OrderPageDTO dto) {
        return Result.success(orderService.page(dto));
    }

    /**
     * 商家端分页查询订单列表，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    @GetMapping("/merchant")
    public Result<IPage<OrderPO>> merchantPage(@RequestHeader("X-Merchant-Id") Long merchantId,
                                               @Valid OrderPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(orderService.page(dto));
    }

    /**
     * 创建订单并完成库存锁定及支付流水初始化。
     *
     * @param dto 订单创建请求参数
     * @return 新创建的订单标识
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(dto.getUserId(), dto.getSkuId(), dto.getQuantity()));
    }

    /**
     * 推进指定待支付订单的支付流程并返回处理结果。
     *
     * @param id 订单标识
     * @return 支付成功返回 true，订单状态不允许支付时返回 false
     */
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable("id") Long id) {
        orderService.pay(id);
        return Result.success(null);
    }

    /**
     * 查询订单状态。
     * @param id 订单ID
     * @return 订单状态
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable("id") Long id) {
        return Result.success(orderService.getStatus(id));
    }

    /**
     * 查询订单详情。
     * @param id 订单ID
     * @return 订单实体
     */
    @GetMapping("/{id}/detail")
    public Result<OrderPO> detail(@PathVariable("id") Long id) {
        return Result.success(orderService.getDetail(id));
    }

    /**
     * 取消订单。
     * @param orderId 订单ID
     * @param userId  当前用户ID
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }

    /**
     * 发货。
     * @param orderId    订单ID
     * @param merchantId 商户ID
     */
    @PostMapping("/{orderId}/ship")
    public Result<Void> ship(@PathVariable Long orderId,
                             @RequestHeader("X-User-Id") Long merchantId) {
        orderService.ship(orderId, merchantId);
        return Result.success(null);
    }

    /**
     * 确认收货。
     * @param orderId 订单ID
     * @param userId  当前用户ID
     */
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirm(@PathVariable Long orderId,
                                @RequestHeader("X-User-Id") Long userId) {
        orderService.confirm(orderId, userId);
        return Result.success(null);
    }

    /**
     * 申请退款。
     * @param orderId 订单ID
     */
    @PostMapping("/{orderId}/refund")
    public Result<Void> refund(@PathVariable Long orderId) {
        orderService.refund(orderId);
        return Result.success(null);
    }

    /**
     * 商家关闭订单，仅允许待支付订单关闭并释放库存。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     */
    @PostMapping("/{orderId}/merchant-close")
    public Result<Void> merchantClose(@PathVariable Long orderId,
                                      @RequestHeader("X-Merchant-Id") Long merchantId) {
        orderService.merchantClose(orderId, merchantId);
        return Result.success(null);
    }

    /**
     * 用户删除订单，仅允许终态订单软删除。
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    @DeleteMapping("/{orderId}")
    public Result<Void> delete(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.delete(orderId, userId);
        return Result.success(null);
    }

    /**
     * 平台强制关闭异常订单，仅允许待支付订单关闭并释放库存。
     * @param orderId 订单ID
     */
    @PostMapping("/{orderId}/platform-close")
    public Result<Void> platformClose(@PathVariable Long orderId) {
        orderService.platformClose(orderId);
        return Result.success(null);
    }

    /**
     * 商家待处理订单数量统计，包含 PENDING/PAID/REFUNDING 三种状态。
     * @param merchantId 商家ID
     * @return 待处理订单总数
     */
    @GetMapping("/merchant/pending-count")
    public Result<Long> merchantPendingCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(orderService.pendingCount(merchantId));
    }

    /**
     * 商家审核退款，merchantId 由可信 Header 注入。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     * @param approved 是否同意退款
     */
    @PostMapping("/{orderId}/refund-review")
    public Result<Void> refundReview(@PathVariable Long orderId,
                                     @RequestHeader("X-Merchant-Id") Long merchantId,
                                     @RequestParam boolean approved) {
        orderService.reviewRefund(orderId, approved, merchantId);
        return Result.success(null);
    }

    /**
     * 平台退款仲裁，强制推进或回退退款状态。
     * @param orderId 订单ID
     * @param approved 仲裁是否支持退款
     */
    @PostMapping("/{orderId}/refund-arbitrate")
    public Result<Void> refundArbitrate(@PathVariable Long orderId,
                                        @RequestParam boolean approved) {
        orderService.arbitrateRefund(orderId, approved);
        return Result.success(null);
    }

    /**
     * 查询订单物流轨迹，按创建时间正序返回。
     * @param orderId 订单ID
     * @return 物流节点列表
     */
    @GetMapping("/{orderId}/logistics")
    public Result<List<OrderLogisticsVO>> logistics(@PathVariable Long orderId) {
        return Result.success(orderService.queryLogistics(orderId));
    }

    private static final String[] ORDER_HEADERS = {
        "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
    };

    /**
     * 商家导出订单 CSV，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 查询入参
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @GetMapping("/export")
    public void export(@RequestHeader("X-Merchant-Id") Long merchantId,
                       @Valid OrderPageDTO dto,
                       HttpServletResponse response) throws IOException {
        List<OrderPO> list = orderService.exportList(merchantId, dto);
        CsvExporter.write(response, "orders.csv", ORDER_HEADERS, toOrderRows(list));
    }

    /**
     * 平台导出全平台订单 CSV。
     * @param dto 查询入参
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @GetMapping("/platform/export")
    public void platformExport(@Valid OrderPageDTO dto,
                               HttpServletResponse response) throws IOException {
        List<OrderPO> list = orderService.platformExportList(dto);
        CsvExporter.write(response, "platform-orders.csv", ORDER_HEADERS, toOrderRows(list));
    }

    /**
     * 平台订单统计聚合，可选 merchantId/时间范围过滤。
     * @param dto 查询入参
     * @return 订单统计VO
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(@Valid OrderPageDTO dto) {
        return Result.success(orderService.statistics(dto));
    }

    private List<String[]> toOrderRows(List<OrderPO> list) {
        List<String[]> rows = new ArrayList<>(list.size());
        for (OrderPO po : list) {
            rows.add(new String[] {
                String.valueOf(po.getId()),
                String.valueOf(po.getUserId()),
                String.valueOf(po.getMerchantId()),
                String.valueOf(po.getSkuId()),
                String.valueOf(po.getQuantity()),
                po.getAmount() == null ? "" : po.getAmount().toPlainString(),
                String.valueOf(po.getStatus()),
                po.getCreateTime() == null ? "" : po.getCreateTime().toString()
            });
        }
        return rows;
    }
}