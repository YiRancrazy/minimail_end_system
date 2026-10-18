package com.yirancrazy.minimall.order.controller.v1;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalOrderControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/order")
public class InternalOrderControllerV1 {

    private final OrderService orderService;

    /**
     * 构造 InternalOrderControllerV1 实例。
     * @param orderService orderService 参数
     */
    public InternalOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }


    /**
     * 执行 status 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable Long id) {
        return Result.success(orderService.getStatus(id));
    }


    /**
     * 执行 merchantId 方法。
     * @param ref ref 参数
     * @return 统一响应结果
     */
    @GetMapping("/merchant/{ref}")
    public Result<Long> merchantId(@PathVariable String ref) {
        return Result.success(orderService.resolveMerchantId(ref));
    }


    /**
     * 执行 paySnapshot 方法。
     * @param ref ref 参数
     * @return 统一响应结果
     */
    @GetMapping("/pay-snapshot/{ref}")
    public Result<OrderPaySnapshotDTO> paySnapshot(@PathVariable String ref) {
        return Result.success(orderService.getPaySnapshot(ref));
    }


    /**
     * 执行 commentEligibility 方法。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/comment-eligibility")
    public Result<OrderCommentEligibilityDTO> commentEligibility(@RequestParam("orderNo") String orderNo,
                                                                 @RequestParam("userId") Long userId,
                                                                 @RequestParam("spuId") Long spuId) {
        return Result.success(orderService.commentEligibility(orderNo, userId, spuId));
    }


    /**
     * 执行 pay 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @PostMapping("/pay/{id}")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success(null);
    }


    /**
     * 执行 payByOrderNo 方法。
     * @param orderNo orderNo 参数
     * @return 统一响应结果
     */
    @PostMapping("/pay-by-order-no/{orderNo}")
    public Result<Void> payByOrderNo(@PathVariable String orderNo) {
        orderService.payByOrderNo(orderNo);
        return Result.success(null);
    }


    /**
     * 执行 refundCallback 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/refund-callback")
    public Result<Void> refundCallback(@Valid @RequestBody RefundCallbackDTO dto) {
        orderService.handleRefundCallback(dto);
        return Result.success(null);
    }


    /**
     * 执行 statistics 方法。
     * @return 统一响应结果
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsDTO> statistics() {
        return Result.success(toStatisticsDto(orderService.statistics(new OrderPageDTO())));
    }


    /**
     * 执行 exportList 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    @GetMapping("/export-list")
    public Result<List<OrderExportItemDTO>> exportList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(orderService.platformExportVOList(startDate, endDate).stream()
            .map(this::toExportItem)
            .toList());
    }


    /**
     * 转换或解析统计Dto。
     * @param vo vo
     * @return 处理结果
     */
    private OrderStatisticsDTO toStatisticsDto(OrderStatisticsVO vo) {
        return new OrderStatisticsDTO(vo.getTotalOrderCount(), vo.getTotalAmount(), vo.getRefundAmount(),
            vo.getPendingCount(), vo.getPaidCount(), vo.getShippedCount(), vo.getReceivedCount(),
            vo.getCancelledCount(), vo.getRefundingCount(), vo.getRefundedCount());
    }


    /**
     * 转换或解析导出Item。
     * @param vo vo
     * @return 处理结果
     */
    private OrderExportItemDTO toExportItem(OrderExportVO vo) {
        return new OrderExportItemDTO(vo.getId(), vo.getUserId(), vo.getMerchantId(), vo.getSkuId(),
            vo.getQuantity(), vo.getAmount(), vo.getStatus(), vo.getCreateTime());
    }
}
