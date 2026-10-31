package com.yirancrazy.minimall.pay.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.PayService;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供支付服务集成测试的 Spring 上下文和外部依赖替身。
 * 加载 test profile、H2、真实 Mapper 及 Mock Feign/支付宝网关；当前仅包含测试配置与夹具方法。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootTest
@ActiveProfiles("test")
class PayServiceImplIntegrationTest {

  private static final AtomicLong NEXT_ID = new AtomicLong(80001L);

  @Autowired private PayService payService;

  @MockBean private AlipayGateway alipayGateway;

  @MockBean private OrderFeignClient orderFeignClient;

  @MockBean private IdFeignClient idFeignClient;

  @SpyBean private PayTransactionMapper payTransactionMapper;

  @Autowired private PayRefundMapper payRefundMapper;

  @Autowired private PayManager payManager;

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 为支付集成测试注册 MyBatis-Plus 元对象填充器。
   * 自动写入创建、更新时间、删除标记和版本字段，使真实 Mapper 持久化满足实体审计约束。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  @org.springframework.boot.test.context.TestConfiguration
  static class TestMetaObjectConfig {

    /**
     * 创建集成测试使用的 MyBatis-Plus 元对象处理器。
     *
     * @return 自动填充审计字段、删除标记和版本的处理器
     */
    @org.springframework.context.annotation.Bean
    com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler() {
      return new com.baomidou.mybatisplus.core.handlers.MetaObjectHandler() {
        public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
          this.strictInsertFill(
              metaObject,
              "createTime",
              java.time.LocalDateTime.class,
              java.time.LocalDateTime.now());
          this.strictInsertFill(
              metaObject,
              "updateTime",
              java.time.LocalDateTime.class,
              java.time.LocalDateTime.now());
          this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
          this.strictInsertFill(metaObject, "version", Integer.class, 0);
        }

        /**
         * 为测试实体更新刷新更新时间。
         *
         * @param metaObject MyBatis-Plus 元对象
         */
        public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
          this.strictUpdateFill(
              metaObject,
              "updateTime",
              java.time.LocalDateTime.class,
              java.time.LocalDateTime.now());
        }
      };
    }
  }

  @BeforeEach
  void setUp() {
    lenient()
        .when(idFeignClient.nextId(anyString()))
        .thenAnswer(invocation -> Result.success(NEXT_ID.getAndIncrement()));
    lenient()
        .when(alipayGateway.createPagePayment(anyString(), any(), anyString(), anyString()))
        .thenReturn("<form action=\"http://pay.url/test\"></form>");
    lenient()
        .when(alipayGateway.refund(anyString(), anyString(), any(), anyString()))
        .thenReturn("REFUND_TRADE_TEST");
    lenient()
        .when(orderFeignClient.paySnapshot(anyString()))
        .thenAnswer(
            invocation -> {
              String orderNo = invocation.getArgument(0);
              if ("ORD-INT-1".equals(orderNo)) {
                return Result.success(
                    new OrderPaySnapshotDto(
                        1L, orderNo, 100L, 200L, new BigDecimal("99.99"), 1, null));
              }
              if ("ORD-INT-OWNER".equals(orderNo)) {
                return Result.success(
                    new OrderPaySnapshotDto(
                        2L, orderNo, 100L, 200L, new BigDecimal("19.99"), 1, null));
              }
              if ("ORD-INT-3".equals(orderNo)) {
                return Result.success(
                    new OrderPaySnapshotDto(3L, orderNo, 1L, 2L, new BigDecimal("50.00"), 1, null));
              }
              if ("ORD-INT-ROLLBACK".equals(orderNo)) {
                return Result.success(
                    new OrderPaySnapshotDto(
                        4L, orderNo, 100L, 200L, new BigDecimal("10.00"), 1, null));
              }
              if ("ORD-INT-SCAN".equals(orderNo)) {
                return Result.success(
                    new OrderPaySnapshotDto(
                        5L, orderNo, 100L, 200L, new BigDecimal("10.00"), 1, null));
              }
              if ("ORD-INT-STATS".equals(orderNo)) {
                return Result.success(
                    new OrderPaySnapshotDto(
                        6L, orderNo, 100L, 77777L, new BigDecimal("100.00"), 1, null));
              }
              return Result.success(null);
            });
  }

  private PayRefundPo successfulRefund(
      String refundNo, String requestNo, String paymentNo, String amount) {
    PayRefundPo refund = new PayRefundPo();
    refund.setRefundNo(refundNo);
    refund.setRefundRequestNo(requestNo);
    refund.setPaymentNo(paymentNo);
    refund.setAmount(new BigDecimal(amount));
    refund.setStatus(1);
    return refund;
  }
}
