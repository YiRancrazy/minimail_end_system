package com.yirancrazy.minimall.order.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.dto.OrderShipDTO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantOrderControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/orders")
public class MerchantOrderControllerV1 {

    private static final String[] ORDER_HEADERS = {
        "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
    };

    private final OrderService orderService;

    /**
     * 构造 MerchantOrderControllerV1 实例。
     * @param orderService orderService 参数
     */
    public MerchantOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<OrderVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                               @Valid OrderPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(orderService.merchantPageVO(dto));
    }


    /**
     * 执行 detail 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId,
                                  @RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(orderService.merchantDetailVO(orderId, merchantId));
    }


    /**
     * 执行 ship 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/ship")
    public Result<Void> ship(@PathVariable Long orderId,
                             @RequestHeader("X-User-Id") Long merchantId,
                             @Valid @RequestBody OrderShipDTO dto) {
        orderService.ship(orderId, merchantId, dto.getCarrier(), dto.getTrackingNo());
        return Result.success(null);
    }


    /**
     * 执行 merchantClose 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/close")
    public Result<Void> merchantClose(@PathVariable Long orderId,
                                      @RequestHeader("X-Merchant-Id") Long merchantId) {
        orderService.merchantClose(orderId, merchantId);
        return Result.success(null);
    }


    /**
     * 执行 pendingCount 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/_count")
    public Result<Long> pendingCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(orderService.pendingCount(merchantId));
    }


    /**
     * 执行 refundReview 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/refund-review")
    public Result<Void> refundReview(@PathVariable Long orderId,
                                     @RequestHeader("X-Merchant-Id") Long merchantId,
                                     @RequestParam boolean approved) {
        orderService.reviewRefund(orderId, approved, merchantId, null);
        return Result.success(null);
    }


    /**
     * 执行 export 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @param response response 参数
     * @throws IOException 处理失败时抛出
     */
    public void export(@RequestHeader("X-Merchant-Id") Long merchantId,
                       @Valid OrderPageDTO dto,
                       HttpServletResponse response) throws IOException {
        List<OrderExportVO> list = orderService.exportVOList(merchantId, dto);
        CsvExporter.write(response, "orders.csv", ORDER_HEADERS, toOrderRows(list));
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
