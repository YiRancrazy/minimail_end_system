package com.yirancrazy.minimall.api.fallback;

import java.util.List;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderFeign Feign 降级工厂，处理OrderFeign服务调用失败降级
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("order-service 不可达: {}", cause.getMessage());
        return new OrderFeignClient() {
            @Override
            public Result<Integer> status(Long id) {
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<Long> merchantId(String ref) {
                log.warn("订单商家解析降级，ref={}，已跳过", ref);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<OrderPaySnapshotDTO> paySnapshot(String ref) {
                log.warn("订单支付快照降级，ref={}，已跳过", ref);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<Void> pay(Long id) {
                log.warn("订单支付降级，orderId={}，已跳过", id);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<Void> payByOrderNo(String orderNo) {
                log.warn("订单按订单号支付降级，orderNo={}，已跳过", orderNo);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<Void> refundCallback(RefundCallbackDTO dto) {
                log.warn("订单退款回调降级，orderId={}，已跳过",
                    dto == null ? null : dto.getOrderId());
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<OrderStatisticsDTO> statistics() {
                log.warn("订单统计降级，返回空统计");
                return Result.success(new OrderStatisticsDTO());
            }

            @Override
            public Result<OrderCommentEligibilityDTO> commentEligibility(String orderNo, Long userId, Long spuId) {
                log.warn("订单评价资格降级，orderNo={}，已拒绝", orderNo);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            @Override
            public Result<List<OrderExportItemDTO>> exportList(String startDate, String endDate) {
                log.warn("订单导出列表降级，startDate={}，endDate={}，已跳过", startDate, endDate);
                return Result.success(List.of());
            }
        };
    }
}
