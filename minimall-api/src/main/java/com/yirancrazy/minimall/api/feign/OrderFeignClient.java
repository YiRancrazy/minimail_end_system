package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Order Feign 客户端，调用Order服务接口
 * @Version: 1.3
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-order-service", fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {
    @GetMapping("/internal/order/{id}")
    Result<Integer> status(@PathVariable("id") Long id);

    /**
     * 按订单标识（订单ID或业务单号）解析订单归属商户ID，供支付服务归属收款商户。
     * @param ref 订单标识
     * @return 商户ID；订单不存在或服务不可达时返回 null
     */
    @GetMapping("/internal/order/merchant/{ref}")
    Result<Long> merchantId(@PathVariable("ref") String ref);

    /**
     * 推进指定订单为已支付状态。
     * @param id 订单ID
     */
    @PostMapping("/internal/order/pay/{id}")
    Result<Void> pay(@PathVariable("id") Long id);

    /**
     * 按业务单号推进订单为已支付状态，供支付回调（C 端携带业务单号）使用。
     * @param orderNo 业务单号
     */
    @PostMapping("/internal/order/pay-by-order-no/{orderNo}")
    Result<Void> payByOrderNo(@PathVariable("orderNo") String orderNo);

    /**
     * Notify the order service of a refund result so it can advance status.
     * @param id the order id
     * @param success whether the refund succeeded
     */
    @PostMapping("/internal/order/refund-callback/{id}")
    Result<Void> refundCallback(@PathVariable("id") Long id, @RequestParam("success") boolean success);
}
