package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在订单服务不可用时按接口语义返回系统错误、空统计或空导出结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
  /**
   * 为订单远程调用异常创建降级客户端.
   *
   * @param cause 订单远程调用异常
   * @return 订单降级客户端
   */
  @Override
  public OrderFeignClient create(Throwable cause) {
    log.warn("order-service 不可达: {}", cause.getMessage());
    return new OrderFeignClient() {
      /**
       * 订单服务不可用时返回系统错误，调用方不得继续状态判断.
       *
       * @param id 订单 ID
       * @return 系统错误响应
       */
      @Override
      public Result<Integer> status(Long id) {
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时返回系统错误并记录订单引用.
       *
       * @param ref 订单 ID 或订单号
       * @return 系统错误响应
       */
      @Override
      public Result<Long> merchantId(String ref) {
        log.warn("订单商家解析降级，ref={}，已跳过", ref);
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时返回系统错误并记录订单引用.
       *
       * @param ref 订单 ID 或订单号
       * @return 系统错误响应
       */
      @Override
      public Result<OrderPaySnapshotDto> paySnapshot(String ref) {
        log.warn("订单支付快照降级，ref={}，已跳过", ref);
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时拒绝按订单 ID 推进支付状态.
       *
       * @param id 订单 ID
       * @return 系统错误响应
       */
      @Override
      public Result<Void> pay(Long id) {
        log.warn("订单支付降级，orderId={}，已跳过", id);
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时拒绝按订单号推进支付状态.
       *
       * @param orderNo 订单号
       * @return 系统错误响应
       */
      @Override
      public Result<Void> payByOrderNo(String orderNo) {
        log.warn("订单按订单号支付降级，orderNo={}，已跳过", orderNo);
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时拒绝应用退款回调并记录订单 ID.
       *
       * @param dto 退款回调数据
       * @return 系统错误响应
       */
      @Override
      public Result<Void> refundCallback(RefundCallbackDto dto) {
        log.warn("订单退款回调降级，orderId={}，已跳过", dto == null ? null : dto.getOrderId());
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时返回空统计对象，避免平台页面报错.
       *
       * @return 空订单统计
       */
      @Override
      public Result<OrderStatisticsDto> statistics() {
        log.warn("订单统计降级，返回空统计");
        return Result.success(new OrderStatisticsDto());
      }

      /**
       * 订单服务不可用时拒绝评价资格并记录订单号.
       *
       * @param orderNo 订单号
       * @param userId 用户 ID
       * @param spuId 商品 SPU ID
       * @return 系统错误响应
       */
      @Override
      public Result<OrderCommentEligibilityDto> commentEligibility(
          String orderNo, Long userId, Long spuId) {
        log.warn("订单评价资格降级，orderNo={}，已拒绝", orderNo);
        return Result.fail(CommonCode.SYS_ERROR, "订单服务不可用");
      }

      /**
       * 订单服务不可用时返回空导出列表，避免平台导出失败.
       *
       * @param startDate 起始日期
       * @param endDate 结束日期
       * @return 空订单明细列表
       */
      @Override
      public Result<List<OrderExportItemDto>> exportList(String startDate, String endDate) {
        log.warn("订单导出列表降级，startDate={}，endDate={}，已跳过", startDate, endDate);
        return Result.success(List.of());
      }
    };
  }
}
