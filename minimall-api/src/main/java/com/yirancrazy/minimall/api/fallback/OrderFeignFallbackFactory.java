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
 * @Description: OrderFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("order-service 不可达: {}", cause.getMessage());
        return new OrderFeignClient() {
            /**
             * 执行 status 方法。
             * @param id id 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Integer> status(Long id) {
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 merchantId 方法。
             * @param ref ref 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Long> merchantId(String ref) {
                log.warn("订单商家解析降级，ref={}，已跳过", ref);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 paySnapshot 方法。
             * @param ref ref 参数
             * @return 统一响应结果
             */
            @Override
            public Result<OrderPaySnapshotDTO> paySnapshot(String ref) {
                log.warn("订单支付快照降级，ref={}，已跳过", ref);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 pay 方法。
             * @param id id 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> pay(Long id) {
                log.warn("订单支付降级，orderId={}，已跳过", id);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 payByOrderNo 方法。
             * @param orderNo orderNo 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> payByOrderNo(String orderNo) {
                log.warn("订单按订单号支付降级，orderNo={}，已跳过", orderNo);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 refundCallback 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> refundCallback(RefundCallbackDTO dto) {
                log.warn("订单退款回调降级，orderId={}，已跳过",
                    dto == null ? null : dto.getOrderId());
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 statistics 方法。
             * @return 统一响应结果
             */
            @Override
            public Result<OrderStatisticsDTO> statistics() {
                log.warn("订单统计降级，返回空统计");
                return Result.success(new OrderStatisticsDTO());
            }

            /**
             * 执行 commentEligibility 方法。
             * @param orderNo orderNo 参数
             * @param userId userId 参数
             * @param spuId spuId 参数
             * @return 统一响应结果
             */
            @Override
            public Result<OrderCommentEligibilityDTO> commentEligibility(String orderNo, Long userId, Long spuId) {
                log.warn("订单评价资格降级，orderNo={}，已拒绝", orderNo);
                return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
            }

            /**
             * 执行 exportList 方法。
             * @param startDate startDate 参数
             * @param endDate endDate 参数
             * @return 统一响应结果
             */
            @Override
            public Result<List<OrderExportItemDTO>> exportList(String startDate, String endDate) {
                log.warn("订单导出列表降级，startDate={}，endDate={}，已跳过", startDate, endDate);
                return Result.success(List.of());
            }
        };
    }
}
