package com.yirancrazy.minimall.api.feign;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-order-service", fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {
    /**
     * 执行 status 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/order/{id}")
    Result<Integer> status(@PathVariable("id") Long id);


    /**
     * 执行 merchantId 方法。
     * @param ref ref 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/order/merchant/{ref}")
    Result<Long> merchantId(@PathVariable("ref") String ref);


    /**
     * 执行 paySnapshot 方法。
     * @param ref ref 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/order/pay-snapshot/{ref}")
    Result<OrderPaySnapshotDTO> paySnapshot(@PathVariable("ref") String ref);


    /**
     * 执行 pay 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/order/pay/{id}")
    Result<Void> pay(@PathVariable("id") Long id);


    /**
     * 执行 payByOrderNo 方法。
     * @param orderNo orderNo 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/order/pay-by-order-no/{orderNo}")
    Result<Void> payByOrderNo(@PathVariable("orderNo") String orderNo);


    /**
     * 执行 refundCallback 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/order/refund-callback")
    Result<Void> refundCallback(@RequestBody RefundCallbackDTO dto);


    /**
     * 执行 statistics 方法。
     * @return 统一响应结果
     */
    @GetMapping("/internal/order/statistics")
    Result<OrderStatisticsDTO> statistics();


    /**
     * 执行 commentEligibility 方法。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/order/comment-eligibility")
    Result<OrderCommentEligibilityDTO> commentEligibility(@RequestParam("orderNo") String orderNo,
                                                          @RequestParam("userId") Long userId,
                                                          @RequestParam("spuId") Long spuId);


    /**
     * 执行 exportList 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/order/export-list")
    Result<List<OrderExportItemDTO>> exportList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);
}
