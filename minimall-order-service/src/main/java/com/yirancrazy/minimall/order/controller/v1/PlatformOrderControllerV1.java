package com.yirancrazy.minimall.order.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端订单控制器，提供平台视角的订单 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@RestController
@RequestMapping("/api/v1/platform/orders")
public class PlatformOrderControllerV1 {

    private static final String[] ORDER_HEADERS = {
        "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
    };

    private final OrderService orderService;

    public PlatformOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 游标分页查询订单列表，支持按用户/商家/状态过滤。
     * @param dto 游标分页查询入参
     * @return 订单游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<OrderVO>> page(@Valid OrderPageDTO dto) {
        return Result.success(orderService.page(dto).map(OrderVO::from));
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
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return Result.success(OrderVO.from(orderService.getDetail(id)));
    }

    /**
     * 平台强制关闭异常订单，仅允许待支付订单关闭并释放库存。
     * @param orderId 订单ID
     */
    @PostMapping("/{orderId}/close")
    public Result<Void> close(@PathVariable Long orderId) {
        orderService.platformClose(orderId);
        return Result.success(null);
    }

    /**
     * 平台退款仲裁，强制推进或回退退款状态。
     * @param orderId 订单ID
     * @param approved 仲裁是否支持退款
     */
    @PostMapping("/{orderId}/arbitrate")
    public Result<Void> refundArbitrate(@PathVariable Long orderId,
                                        @RequestParam boolean approved) {
        orderService.arbitrateRefund(orderId, approved);
        return Result.success(null);
    }

    /**
     * 平台导出全平台订单 CSV。
     * @param dto 查询入参
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @GetMapping("/export")
    public void export(@Valid OrderPageDTO dto,
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
