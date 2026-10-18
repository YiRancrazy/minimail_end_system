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
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderSummaryVO;
import com.yirancrazy.minimall.order.vo.OrderTrendVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformOrderControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/orders")
public class PlatformOrderControllerV1 {

    private static final String[] ORDER_HEADERS = {
        "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
    };

    private final OrderService orderService;

    /**
     * 构造 PlatformOrderControllerV1 实例。
     * @param orderService orderService 参数
     */
    public PlatformOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<OrderVO>> page(@Valid OrderPageDTO dto) {
        return Result.success(orderService.pageVO(dto));
    }


    /**
     * 执行 status 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable("id") Long id) {
        return Result.success(orderService.getStatus(id));
    }


    /**
     * 执行 detail 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}/detail")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return Result.success(orderService.platformDetailVO(id));
    }


    /**
     * 关闭相关资源。
     * @param orderId orderId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/close")
    public Result<Void> close(@PathVariable Long orderId) {
        orderService.platformClose(orderId);
        return Result.success(null);
    }


    /**
     * 执行 refundArbitrate 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/arbitrate")
    public Result<Void> refundArbitrate(@PathVariable Long orderId,
                                        @RequestParam boolean approved) {
        orderService.arbitrateRefund(orderId, approved);
        return Result.success(null);
    }


    /**
     * 执行 export 方法。
     * @param dto dto 参数
     * @param response response 参数
     * @throws IOException 处理失败时抛出
     */
    public void export(@Valid OrderPageDTO dto,
                               HttpServletResponse response) throws IOException {
        List<OrderExportVO> list = orderService.platformExportVOList(dto);
        CsvExporter.write(response, "platform-orders.csv", ORDER_HEADERS, toOrderRows(list));
    }


    /**
     * 执行 statistics 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(@Valid OrderPageDTO dto) {
        return Result.success(orderService.statistics(dto));
    }


    /**
     * 执行 summary 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/summary")
    public Result<OrderSummaryVO> summary(@RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate,
                                          @RequestParam(required = false) String merchantId) {
        return Result.success(orderService.orderSummary(startDate, endDate, merchantId));
    }


    /**
     * 执行 trend 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/trend")
    public Result<OrderTrendVO> trend(@RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate,
                                      @RequestParam(required = false) String merchantId) {
        return Result.success(orderService.orderTrend(startDate, endDate, merchantId));
    }

    /**
     * 转换或解析订单Rows。
     * @param list 列表
     * @return 结果列表
     */
    private List<String[]> toOrderRows(List<OrderExportVO> list) {
        List<String[]> rows = new ArrayList<>(list.size());
        for (OrderExportVO vo : list) {
            rows.add(new String[] {
                String.valueOf(vo.getId()),
                String.valueOf(vo.getUserId()),
                String.valueOf(vo.getMerchantId()),
                String.valueOf(vo.getSkuId()),
                String.valueOf(vo.getQuantity()),
                vo.getAmount() == null ? "" : vo.getAmount().toPlainString(),
                String.valueOf(vo.getStatus()),
                vo.getCreateTime() == null ? "" : vo.getCreateTime().toString()
            });
        }
        return rows;
    }
}
