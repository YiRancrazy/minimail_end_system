package com.yirancrazy.minimall.pay.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDto;
import com.yirancrazy.minimall.pay.dto.PayCallbackDto;
import com.yirancrazy.minimall.pay.dto.PayPageDto;
import com.yirancrazy.minimall.pay.dto.PayStatementDto;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDto;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDto;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.gateway.PayRemoteGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;
import com.yirancrazy.minimall.pay.vo.PayStatementVo;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVo;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVo;
import com.yirancrazy.minimall.pay.vo.RefundVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 覆盖支付服务核心业务分支的 Mockito 单元测试集合。
 * 以 Manager、Gateway、远程网关和事务管理器替身验证支付创建、回调、退款、查询与补偿行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class PayServiceImplTest {

  private PayManager manager;
  private PayRefundManager payRefundManager;
  private PayGateway payGateway;
  private PayRemoteGateway payRemoteGateway;
  private PlatformTransactionManager transactionManager;
  private PayServiceImpl service;

  @BeforeAll
  static void initLambdaCache() {
    // 无 MyBatis 容器时 LambdaQueryWrapper 列名解析/参数绑定懒执行到 getSqlSegment 才触发，
    // 需预置 TableInfo 的 lambda 缓存，否则 wrapper.getSqlSegment() 抛 "can not find lambda cache"
    MybatisConfiguration configuration = new MybatisConfiguration();
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(configuration, ""), PayTransactionPo.class);
    TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PayRefundPo.class);
  }

  @BeforeEach
  void setUp() {
    manager = mock(PayManager.class);
    payRefundManager = mock(PayRefundManager.class);
    payGateway = mock(PayGateway.class);
    payRemoteGateway = mock(PayRemoteGateway.class);
    transactionManager = mock(PlatformTransactionManager.class);
    lenient()
        .when(transactionManager.getTransaction(any()))
        .thenReturn(new SimpleTransactionStatus());
    lenient().when(payRemoteGateway.nextId(anyString())).thenReturn(Result.success(90001L));
    lenient()
        .when(payRemoteGateway.paySnapshot(anyString()))
        .thenAnswer(
            invocation -> {
              String orderNo = invocation.getArgument(0);
              if ("ORDER100".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 1L, 1L, "99.99", 1));
              }
              if ("ORDER200".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 2L, 3L, "50.00", 1));
              }
              if ("ORDER300".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 3L, null, "10.00", 1));
              }
              if ("ORDER400".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 4L, 42L, "10.00", 1));
              }
              return Result.success(null);
            });
    lenient().when(payRemoteGateway.pay(anyLong())).thenReturn(Result.success(null));
    lenient().when(payRemoteGateway.payByOrderNo(anyString())).thenReturn(Result.success(null));
    lenient()
        .when(
            manager.markSuccessIfPending(
                anyString(), anyString(), anyString(), any(LocalDateTime.class), anyInt(), anyInt()))
        .thenReturn(1);
    lenient()
        .when(payRemoteGateway.refundCallback(any(RefundCallbackDto.class)))
        .thenReturn(Result.success(null));
    lenient().when(manager.updateById(any(PayTransactionPo.class))).thenReturn(true);
    lenient()
        .when(
            payGateway.createPagePayment(
                anyString(), any(BigDecimal.class), anyString(), anyString()))
        .thenReturn(
            "<form action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do\">mock</form>");
    doAnswer(
            inv -> {
              PayTransactionPo p = inv.getArgument(0);
              if (p.getId() == null) {
                p.setId(System.nanoTime());
              }
              return true;
            })
        .when(manager)
        .save(any(PayTransactionPo.class));
    service =
        new PayServiceImpl(
            manager, payRefundManager, payGateway, payRemoteGateway, transactionManager);
  }

  /**
   * 验证首次创建支付保存待支付流水且不提前生成支付宝表单。
   * 订单快照用户和金额匹配时，应保存状态 1、金额 `99.99`、活跃订单号并返回非空 ID。
   */
  public void createPaymentPersistsPendingRecord() {
    Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);
    assertNotNull(id);
    ArgumentCaptor<PayTransactionPo> cap = ArgumentCaptor.forClass(PayTransactionPo.class);
    verify(manager).save(cap.capture());
    assertEquals(1, cap.getValue().getStatus());
    assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
    assertEquals("ORDER100", cap.getValue().getActiveOrderNo());
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /**
   * 验证创建支付拒绝不属于当前用户的订单。
   * 订单快照用户为 1、请求用户为 999 时，应抛出业务异常且不创建支付流水。
   */
  public void createPaymentRejectsOtherUser() {
    assertThrows(
        BizException.class,
        () -> service.createPayment("ORDER100", 999L, 1L, new BigDecimal("99.99"), null));
  }

  /**
   * 验证创建支付拒绝与订单应付金额不一致的请求。
   * 请求金额 `0.01` 与快照金额 `99.99` 不同，应抛出金额不一致业务异常。
   */
  public void createPaymentRejectsAmountMismatch() {
    assertThrows(
        BizException.class,
        () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("0.01"), null));
  }

  /**
   * 验证创建支付将等值金额规范为两位小数。
   * 输入 `99.990` 时，落库金额 scale 应为 2，数值仍等于 `99.99`。
   */
  public void createPaymentNormalizesAmountScale() {
    service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.990"), null);

    ArgumentCaptor<PayTransactionPo> cap = ArgumentCaptor.forClass(PayTransactionPo.class);
    verify(manager).save(cap.capture());
    assertEquals(2, cap.getValue().getAmount().scale());
    assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
  }

  /**
   * 验证创建支付拒绝超过两位小数的金额。
   * 输入 `99.991` 应在订单金额一致性校验阶段失败，错误码为金额不一致且不保存流水。
   */
  public void createPaymentRejectsMoreThanTwoDecimals() {
    BizException ex =
        assertThrows(
            BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.991"), null));

    assertEquals(PayCodeEnum.PAY_AMOUNT_MISMATCH.getCode(), ex.getCode());
    verify(manager, never()).save(any(PayTransactionPo.class));
  }

  /**
   * 验证 ID 服务失败时创建支付终止。
   * ID 网关返回失败后，应抛出全局 ID 生成失败业务异常，且不保存支付流水。
   */
  public void createPaymentFailsWhenIdServiceUnavailable() {
    when(payRemoteGateway.nextId(anyString())).thenReturn(Result.fail("20000", "ID服务不可用"));

    BizException ex =
        assertThrows(
            BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null));

    assertEquals(PayCodeEnum.PAY_ID_GENERATE_FAIL.getCode(), ex.getCode());
    verify(manager, never()).save(any(PayTransactionPo.class));
  }

  /**
   * 验证非待支付订单不能创建支付。
   * 订单快照状态为 2 时，应抛出订单状态非法业务异常。
   */
  public void createPaymentRejectsNonPendingOrder() {
    when(payRemoteGateway.paySnapshot("ORDER500"))
        .thenReturn(Result.success(snapshot("ORDER500", 5L, 1L, "12.00", 2)));
    when(manager.getOne(any())).thenReturn(null);

    assertThrows(
        BizException.class,
        () -> service.createPayment("ORDER500", 5L, 1L, new BigDecimal("12.00"), null));
  }

  /**
   * 验证未过期待支付流水按订单号幂等复用。
   * 已有状态 1 且未过期流水时，应返回原 ID，不保存新流水，也不生成支付表单。
   */
  public void createPaymentReusesPendingTransaction() {
    PayTransactionPo existing = new PayTransactionPo();
    existing.setId(99L);
    existing.setOrderNo("ORDER100");
    existing.setStatus(1); // 待支付（PENDING）
    existing.setExpireAt(LocalDateTime.now().plusMinutes(10));
    when(manager.getOne(any())).thenReturn(existing);

    Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

    assertEquals(99L, id.longValue());
    verify(manager, never()).save(any(PayTransactionPo.class));
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /**
   * 验证过期待支付流水关闭后创建新支付单。
   * 旧流水状态 1 且已过期时，应将其置为 4、清空活跃订单号，并保存新的待支付流水。
   */
  public void createPaymentCreatesNewWhenPendingExpired() {
    PayTransactionPo existing = new PayTransactionPo();
    existing.setId(99L);
    existing.setOrderNo("ORDER100");
    existing.setStatus(1); // 待支付（PENDING）
    existing.setExpireAt(LocalDateTime.now().minusMinutes(1));
    when(manager.getOne(any())).thenReturn(existing);

    Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

    assertNotNull(id);
    assertNotEquals(99L, id.longValue());
    assertEquals(4, existing.getStatus());
    assertNull(existing.getActiveOrderNo());
    verify(manager).updateById(existing);
    verify(manager).save(any(PayTransactionPo.class));
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /**
   * 验证活跃订单唯一键冲突时返回并发创建的有效流水。
   * 插入抛出 `DuplicateKeyException` 后，应重新查询并返回状态 1 的已有支付单 ID 88。
   */
  public void createPaymentDuplicateActiveKeyReusesExisting() {
    PayTransactionPo active = new PayTransactionPo();
    active.setId(88L);
    active.setOrderNo("ORDER100");
    active.setPaymentNo("PAY-ACTIVE");
    active.setStatus(1);
    when(manager.getOne(any())).thenReturn(null, active);
    when(manager.save(any(PayTransactionPo.class)))
        .thenThrow(new DuplicateKeyException("duplicate active order"));

    Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

    assertEquals(88L, id.longValue());
  }

  /**
   * 验证已成功支付流水始终幂等复用。
   * 同订单已有状态 2 流水时，应返回原 ID 且不再创建新单。
   */
  public void createPaymentReusesSuccessTransaction() {
    PayTransactionPo existing = new PayTransactionPo();
    existing.setId(88L);
    existing.setOrderNo("ORDER100");
    existing.setStatus(2); // 支付成功（SUCCESS）
    when(manager.getOne(any())).thenReturn(existing);

    Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

    assertEquals(88L, id.longValue());
    verify(manager, never()).save(any(PayTransactionPo.class));
  }

  /**
   * 验证失败支付流水可以被新支付请求替代。
   * 同订单已有状态 3 流水时，应保存新待支付流水并返回不同 ID。
   */
  public void createPaymentCreatesNewAfterFailed() {
    PayTransactionPo existing = new PayTransactionPo();
    existing.setId(77L);
    existing.setOrderNo("ORDER100");
    existing.setStatus(3); // 支付失败（FAILED）
    when(manager.getOne(any())).thenReturn(existing);

    Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

    assertNotNull(id);
    assertNotEquals(77L, id.longValue());
    verify(manager).save(any(PayTransactionPo.class));
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /**
   * 验证当前服务拒绝非支付宝渠道。
   * 请求渠道 code 2 时，应抛出渠道不支持业务异常，不读取订单快照或保存流水。
   */
  public void createPaymentUnsupportedChannelThrows() {
    assertThrows(
        BizException.class,
        () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), 2));
  }

  /**
   * 验证成功回调更新支付状态并推进数字订单。
   * 待支付流水收到成功回调后，状态应变为 2，并调用订单服务 `pay(100)`。
   */
  public void handleCallbackMarksSuccess() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(1);
    rec.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", true, "response");
    service.handleCallback(dto);
    assertEquals(2, rec.getStatus());
    verify(payRemoteGateway).pay(100L);
  }

  /**
   * 验证退款中流水忽略迟到的成功回调。
   * 状态 5 流水收到成功回调后应保持 5，不更新支付记录，也不推进订单。
   */
  public void handleCallbackRefundingPaymentDoesNotAdvanceOrder() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(5);
    rec.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", true, "response");
    service.handleCallback(dto);

    assertEquals(5, rec.getStatus());
    verify(payRemoteGateway, never()).pay(anyLong());
    verify(payRemoteGateway, never()).payByOrderNo(anyString());
    verify(manager, never()).updateById(rec);
  }

  /**
   * 验证已退款流水忽略迟到的成功回调。
   * 状态 6 流水收到成功回调后应保持 6，不更新支付记录，也不推进订单。
   */
  public void handleCallbackRefundedPaymentDoesNotAdvanceOrder() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(6);
    rec.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", true, "response");
    service.handleCallback(dto);

    assertEquals(6, rec.getStatus());
    verify(payRemoteGateway, never()).pay(anyLong());
    verify(payRemoteGateway, never()).payByOrderNo(anyString());
    verify(manager, never()).updateById(rec);
  }

  /**
   * 验证冻结流水忽略迟到的成功回调。
   * 状态 7 流水收到成功回调后应保持 7，不更新支付记录，也不推进订单。
   */
  public void handleCallbackFrozenPaymentDoesNotAdvanceOrder() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(7);
    rec.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", true, "response");
    service.handleCallback(dto);

    assertEquals(7, rec.getStatus());
    verify(payRemoteGateway, never()).pay(anyLong());
    verify(payRemoteGateway, never()).payByOrderNo(anyString());
    verify(manager, never()).updateById(rec);
  }

  /**
   * 验证非数字订单号按字符串调用订单推进接口。
   * 流水订单号为 `OD20260814001` 时，成功回调应调用 `payByOrderNo` 而非数字 ID 接口。
   */
  public void handleCallbackAdvancesOrderByOrderNo() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(1);
    rec.setOrderNo("OD20260814001");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", true, "response");
    service.handleCallback(dto);
    assertEquals(2, rec.getStatus());
    verify(payRemoteGateway).payByOrderNo("OD20260814001");
  }

  /**
   * 验证支付单不存在时回调不能静默成功。
   * 查询不到支付流水应抛出 `BizException`，防止渠道收到虚假确认。
   */
  public void handleCallbackMissingThrowsBiz() {
    when(manager.getOne(any())).thenReturn(null);
    PayCallbackDto dto = new PayCallbackDto("PAY999", "TRADE999", true, "response");
    assertThrows(BizException.class, () -> service.handleCallback(dto));
  }

  /**
   * 验证待支付流水的失败回调只更新本地状态。
   * 待支付流水收到失败结果后应变为状态 3，且不调用订单推进接口。
   */
  public void handleCallbackMarksFailed() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(1);
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", false, "response");
    service.handleCallback(dto);
    assertEquals(3, rec.getStatus());
    verify(payRemoteGateway, never()).pay(any());
  }

  /**
   * 验证按订单号查询返回当前用户拥有的最新流水。
   * 查询命中订单 `ORDER100` 且用户匹配时，返回结果的 ID 和订单号应一致。
   */
  public void getByOrderNoReturnsRecordWhenExists() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setOrderNo("ORDER100");
    rec.setUserId(1L);
    when(manager.getOne(any())).thenReturn(rec);

    PayTransactionPo result = service.getByOrderNo("ORDER100", 1L);
    assertEquals(1L, result.getId());
    assertEquals("ORDER100", result.getOrderNo());
  }

  /**
   * 验证订单没有支付流水时查询失败。
   * 查询不存在的 `ORDER999` 应抛出业务异常。
   */
  public void getByOrderNoMissingThrowsBiz() {
    when(manager.getOne(any())).thenReturn(null);
    assertThrows(BizException.class, () -> service.getByOrderNo("ORDER999", 1L));
  }

  /**
   * 验证订单支付流水不能跨用户读取。
   * 流水属于用户 1、请求用户为 999 时应抛出支付单不存在业务异常。
   */
  public void getByOrderNoRejectsOtherUser() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setOrderNo("ORDER100");
    rec.setUserId(1L);
    when(manager.getOne(any())).thenReturn(rec);

    BizException ex =
        assertThrows(BizException.class, () -> service.getByOrderNo("ORDER100", 999L));
    assertEquals(PayCodeEnum.PAY_NOT_FOUND.getCode(), ex.getCode());
  }

  /**
   * 验证收银台参数聚合支付单信息并生成表单。
   * 支付单属于当前用户时，返回支付单号、订单号、金额、标题和网关表单。
   */
  public void getPaymentParamsReturnsVoWhenExists() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setPaymentNo("PAY123");
    rec.setOrderNo("ORDER100");
    rec.setUserId(1L);
    rec.setAmount(new BigDecimal("99.99"));
    rec.setCurrency("CNY");
    rec.setChannel(1);
    rec.setExpireAt(LocalDateTime.now().plusMinutes(15));
    when(manager.getOne(any())).thenReturn(rec);

    PaymentParamsVo vo = service.getPaymentParams("PAY123", 1L);
    assertEquals("PAY123", vo.getPaymentNo());
    assertEquals("ORDER100", vo.getOrderNo());
    assertEquals(0, new BigDecimal("99.99").compareTo(vo.getAmount()));
    assertEquals("Order ORDER100", vo.getSubject());
    assertEquals(
        "<form action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do\">mock</form>",
        vo.getPayForm());
  }

  /**
   * 验证支付宝表单生成失败不会被服务层吞掉。
   * 网关抛出运行时异常时，收银台参数接口应向调用方传播异常。
   */
  public void getPaymentParamsGatewayFailureThrows() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setPaymentNo("PAY123");
    rec.setOrderNo("ORDER100");
    rec.setUserId(1L);
    rec.setAmount(new BigDecimal("99.99"));
    rec.setExpireAt(LocalDateTime.now().plusMinutes(15));
    when(manager.getOne(any())).thenReturn(rec);
    when(payGateway.createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString()))
        .thenThrow(new RuntimeException("alipay down"));

    assertThrows(RuntimeException.class, () -> service.getPaymentParams("PAY123", 1L));
  }

  /**
   * 验证支付单不存在时不能生成收银台参数。
   * 查询不到 `PAY999` 应抛出业务异常。
   */
  public void getPaymentParamsMissingThrowsBiz() {
    when(manager.getOne(any())).thenReturn(null);
    assertThrows(BizException.class, () -> service.getPaymentParams("PAY999", 1L));
  }

  /**
   * 验证收银台参数查询执行支付单归属校验。
   * 支付单属于用户 1、请求用户为 999 时应拒绝访问，且不调用支付宝网关。
   */
  public void getPaymentParamsRejectsOtherUser() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setPaymentNo("PAY123");
    rec.setUserId(1L);
    when(manager.getOne(any())).thenReturn(rec);

    BizException ex =
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY123", 999L));
    assertEquals(PayCodeEnum.PAY_NOT_FOUND.getCode(), ex.getCode());
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /**
   * 验证商家流水分页将查询委托给 Manager 并返回游标页。
   * 状态筛选为 2 且 Manager 返回空列表时，结果应为非空游标页并调用一次 `list`。
   */
  public void pageDelegatesToManager() {
    PayPageDto dto = new PayPageDto();
    dto.setStatus(2);
    List<PayTransactionPo> mockRecords = new ArrayList<>();
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(mockRecords);

    CursorPageVo<PayTransactionPo> result = service.page(10L, dto);
    assertNotNull(result);
    verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 验证平台流水分页不附加商家条件并委托 Manager 查询。
   * Manager 返回空列表时，应得到非空游标页并调用一次 `list`。
   */
  public void platformPageDelegatesToManager() {
    PayPageDto dto = new PayPageDto();
    List<PayTransactionPo> mockRecords = new ArrayList<>();
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(mockRecords);

    CursorPageVo<PayTransactionPo> result = service.platformPage(dto);
    assertNotNull(result);
    verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 验证统计 DTO 被完整转换为统计视图。
   * Manager 返回总额 1000、退款 100、50 笔时，视图三个字段应保持一致。
   */
  public void statisticsDelegatesToMapper() {
    PayPageDto dto = new PayPageDto();
    PayStatisticsDto expected =
        new PayStatisticsDto(new BigDecimal("1000.00"), new BigDecimal("100.00"), 50L);
    when(manager.statistics(any(), any(), any())).thenReturn(expected);

    PayStatisticsVo result = service.statistics(10L, dto);
    assertEquals(0, new BigDecimal("1000.00").compareTo(result.getTotalAmount()));
    assertEquals(0, new BigDecimal("100.00").compareTo(result.getRefundAmount()));
    assertEquals(50L, result.getTransactionCount());
    verify(manager).statistics(any(), any(), any());
  }

  /**
   * 验证对账单聚合结果完整映射到视图。
   * 服务应把总数、支付、退款和冻结三个维度的局部合计逐一复制。
   */
  public void statementDelegatesToMapper() {
    PayStatementDto dto = new PayStatementDto();
    dto.setStartDate(LocalDate.of(2026, 7, 1));
    dto.setEndDate(LocalDate.of(2026, 7, 31));

    PayStatementResultDto expected = new PayStatementResultDto();
    expected.setTotalCount(100L);
    expected.setTotalAmount(new BigDecimal("10000.00"));
    expected.setPaidCount(80L);
    expected.setPaidAmount(new BigDecimal("8000.00"));
    expected.setRefundedCount(5L);
    expected.setRefundedAmount(new BigDecimal("500.00"));
    expected.setFrozenCount(2L);
    expected.setFrozenAmount(new BigDecimal("200.00"));
    when(manager.statement(any(), any())).thenReturn(expected);

    PayStatementVo result = service.statement(dto);

    assertEquals(100L, result.getTotalCount());
    assertEquals(0, new BigDecimal("10000.00").compareTo(result.getTotalAmount()));
    assertEquals(80L, result.getPaidCount());
    assertEquals(0, new BigDecimal("8000.00").compareTo(result.getPaidAmount()));
    assertEquals(5L, result.getRefundedCount());
    assertEquals(0, new BigDecimal("500.00").compareTo(result.getRefundedAmount()));
    assertEquals(2L, result.getFrozenCount());
    assertEquals(0, new BigDecimal("200.00").compareTo(result.getFrozenAmount()));
    verify(manager).statement(any(), any());
  }

  /**
   * 验证冻结已存在支付单时更新为状态 7。
   * 查询命中 `PAY123` 后，应修改实体状态并调用 `updateById` 持久化。
   */
  public void freezeMarksFrozenWhenExists() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setPaymentNo("PAY123");
    rec.setStatus(1);
    when(manager.getOne(any())).thenReturn(rec);

    service.freeze("PAY123");
    assertEquals(7, rec.getStatus());
    verify(manager).updateById(rec);
  }

  /**
   * 验证冻结不存在的支付单时失败。
   * 查询不到 `PAY999` 应抛出业务异常，不执行无目标更新。
   */
  public void freezeMissingThrowsBiz() {
    when(manager.getOne(any())).thenReturn(null);
    assertThrows(BizException.class, () -> service.freeze("PAY999"));
  }

  /**
   * 验证空筛选导出返回 Manager 的空结果。
   * Manager 返回空列表时，`exportTransactions` 应返回大小为 0 的列表。
   */
  public void exportTransactionsDelegatesToManager() {
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(new java.util.ArrayList<>());
    assertEquals(0, service.exportTransactions(new PayPageDto()).size());
  }

  /**
   * 验证退款引用的支付单不存在时失败。
   * 按支付 ID 查询为空应抛出业务异常，不生成退款流水。
   */
  public void createRefundPayNotFoundThrows() {
    when(manager.getById(any())).thenReturn(null);
    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("50.00"), "test");
    assertThrows(BizException.class, () -> service.createRefund(dto));
  }

  /**
   * 验证只有成功支付单可以发起退款。
   * 支付单状态为待支付 1 时，应抛出支付未成功业务异常。
   */
  public void createRefundPayNotSuccessThrows() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(1); // 待支付，不是支付成功（SUCCESS，2）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    when(manager.getById(1L)).thenReturn(payTx);

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("50.00"), "test");
    assertThrows(BizException.class, () -> service.createRefund(dto));
  }

  /**
   * 验证单次退款金额不能超过支付金额。
   * 对 50 元支付单申请 100 元退款时，应抛出退款金额超限业务异常。
   */
  public void createRefundAmountExceedThrows() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("50.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("100.00"), "test");
    assertThrows(BizException.class, () -> service.createRefund(dto));
  }

  /**
   * 验证退款金额最多保留两位小数。
   * 申请 `10.005` 时应抛出金额精度非法业务异常，且不调用渠道退款。
   */
  public void createRefundRejectsMoreThanTwoDecimals() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("50.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("10.005"), "test");

    BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));
    assertEquals(PayCodeEnum.PAY_AMOUNT_SCALE_INVALID.getCode(), ex.getCode());
    verify(payGateway, never())
        .refund(anyString(), anyString(), any(BigDecimal.class), anyString());
  }

  /**
   * 验证退款 ID 生成失败时终止流程。
   * ID 网关返回 null 时，应抛出全局 ID 生成失败异常，且不调用渠道退款。
   */
  public void createRefundFailsWhenIdServiceUnavailable() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("50.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(payRemoteGateway.nextId(anyString())).thenReturn(null);

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("10.00"), "test");

    BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));
    assertEquals(PayCodeEnum.PAY_ID_GENERATE_FAIL.getCode(), ex.getCode());
    verify(payGateway, never())
        .refund(anyString(), anyString(), any(BigDecimal.class), anyString());
  }

  /**
   * 验证部分退款成功后返回退款视图并通知订单。
   * 渠道退款成功后，视图应包含新退款号、支付单号、50 元金额，并回传未全额退款标记。
   */
  public void createRefundSuccessReturnsVo() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.getOne(any())).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123"))
        .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
    when(payRefundManager.updateStatusIf(anyString(), eq(0), eq(1), anyString())).thenReturn(1);
    when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
        .thenReturn("REFUND_TRADE_123");

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("50.00"), "reason");
    com.yirancrazy.minimall.pay.vo.RefundVo vo = service.createRefund(dto);

    assertNotNull(vo);
    assertNotNull(vo.getRefundNo());
    assertEquals("PAY123", vo.getPaymentNo());
    assertEquals(0, new BigDecimal("50.00").compareTo(vo.getAmount()));
    ArgumentCaptor<RefundCallbackDto> callback = ArgumentCaptor.forClass(RefundCallbackDto.class);
    verify(payRemoteGateway).refundCallback(callback.capture());
    assertEquals(
        0, new BigDecimal("50.00").compareTo(callback.getValue().getTotalRefundedAmount()));
    assertEquals(false, callback.getValue().getFullyRefunded());
  }

  /**
   * 验证并发退款抢占支付状态失败时回滚。
   * 原状态条件更新返回 0 时，应抛出退款冲突、回滚独立事务，且不调用渠道或订单回调。
   */
  public void createRefundAtomicClaimFailsWhenNotSuccess() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 读到 SUCCESS，但行级抢占已被并发退款先行置为 REFUNDING
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(0);
    when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("0.00"));

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("50.00"), "test");
    BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));

    assertEquals("40007", ex.getCode());
    verify(payRefundManager).save(any(PayRefundPo.class));
    verify(transactionManager).rollback(any());
    verify(payGateway, never())
        .refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDto.class));
  }

  /**
   * 验证累计退款金额不能超过原支付金额。
   * 已有 80 元退款占用时再申请 30 元，应抛出累计超限、回滚事务且不保存新退款。
   */
  public void createRefundRejectsWhenCumulativeExceeds() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("80.00"));

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("30.00"), "test");
    BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));

    assertEquals("40008", ex.getCode());
    verify(payRefundManager, never()).save(any(PayRefundPo.class));
    verify(manager, never()).updateStatusIf("PAY123", 5, 2);
    verify(transactionManager).rollback(any());
    verify(payGateway, never())
        .refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDto.class));
  }

  /**
   * 验证新退款先以待处理落库，成功后更新状态并通知订单。
   * 应记录支付单、请求号和金额，最终返回成功，未退满时把支付单恢复为成功状态。
   */
  public void createRefundSuccessRecordsRefund() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.getOne(any())).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123"))
        .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
    when(payRefundManager.updateStatusIf("REFUND90001", 0, 1, "REFUND_TRADE_123")).thenReturn(1);
    when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
        .thenReturn("REFUND_TRADE_123");

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1");
    // insert 时点快照：同一退款对象后续会被服务置为 SUCCESS，需在插入瞬间记录状态
    final int[] insertStatusSnapshot = new int[1];
    doAnswer(
            inv -> {
              PayRefundPo p = inv.getArgument(0);
              insertStatusSnapshot[0] = p.getStatus();
              return true;
            })
        .when(payRefundManager)
        .save(any(PayRefundPo.class));

    RefundVo vo = service.createRefund(dto);

    assertEquals(1, vo.getStatus()); // 退款记录最终 SUCCESS
    ArgumentCaptor<PayRefundPo> cap = ArgumentCaptor.forClass(PayRefundPo.class);
    verify(payRefundManager).save(cap.capture());
    assertEquals("PAY123", cap.getValue().getPaymentNo());
    assertEquals(0, insertStatusSnapshot[0]); // 落库时为 PENDING
    verify(payRefundManager).updateStatusIf("REFUND90001", 0, 1, "REFUND_TRADE_123");
    assertEquals(1, cap.getValue().getStatus()); // 成功后 SUCCESS
    assertEquals("REFUND-REQ-1", cap.getValue().getRefundRequestNo());
    verify(manager).updateStatusIf("PAY123", 5, 2); // 未退满回 SUCCESS
    ArgumentCaptor<RefundCallbackDto> callback = ArgumentCaptor.forClass(RefundCallbackDto.class);
    verify(payRemoteGateway).refundCallback(callback.capture());
    assertEquals("REFUND-REQ-1", callback.getValue().getRefundRequestNo());
    assertEquals(false, callback.getValue().getFullyRefunded());
  }

  /**
   * 验证相同退款请求号的最终状态结果直接复用。
   * 已有成功退款应返回原退款号，不重新读取支付单、不再次调用渠道或订单回调。
   */
  public void createRefundReusesExistingRequest() {
    PayRefundPo existing = new PayRefundPo();
    existing.setRefundNo("REFUND-EXISTING");
    existing.setRefundRequestNo("REFUND-REQ-1");
    existing.setPaymentNo("PAY123");
    existing.setAmount(new BigDecimal("50.00"));
    existing.setStatus(1);
    when(payRefundManager.findByRefundRequestNo("REFUND-REQ-1")).thenReturn(existing);

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1");
    RefundVo result = service.createRefund(dto);

    assertEquals("REFUND-EXISTING", result.getRefundNo());
    verify(manager, never()).getById(anyLong());
    verify(manager, never()).updateStatusIf(anyString(), anyInt(), anyInt());
    verify(payGateway, never())
        .refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDto.class));
  }

  /**
   * 验证待处理退款在渠道无记录时使用原退款号重提。
   * 查询返回 `NOT_FOUND` 后应调用同一退款号的渠道退款，并最终更新退款及支付状态。
   */
  public void createRefundRecoversExistingPendingRequest() {
    PayRefundPo existing = new PayRefundPo();
    existing.setRefundNo("REFUND-STABLE");
    existing.setRefundRequestNo("REFUND-REQ-1");
    existing.setPaymentNo("PAY123");
    existing.setAmount(new BigDecimal("50.00"));
    existing.setReason("reason");
    existing.setStatus(0);
    when(payRefundManager.findByRefundRequestNo("REFUND-REQ-1")).thenReturn(existing);

    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2);
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.getOne(any())).thenReturn(payTx);
    when(payGateway.queryRefund("PAY123", "REFUND-STABLE"))
        .thenReturn(PayGateway.RefundQueryStatus.NOT_FOUND);
    when(payGateway.refund("PAY123", "REFUND-STABLE", new BigDecimal("50.00"), "reason"))
        .thenReturn("REFUND_TRADE_123");
    when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
    when(payRefundManager.updateStatusIf(eq("REFUND-STABLE"), eq(0), eq(1), any())).thenReturn(1);

    RefundVo result =
        service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1"));

    assertEquals("REFUND-STABLE", result.getRefundNo());
    assertEquals(1, result.getStatus());
    verify(payGateway).refund("PAY123", "REFUND-STABLE", new BigDecimal("50.00"), "reason");
    verify(manager).updateStatusIf("PAY123", 5, 2);
  }

  /**
   * 验证渠道退款异常时保留待处理流水供补偿。
   * 网关超时应抛业务异常，退款状态仍为 0，不标记成功、不回退支付状态，也不通知订单。
   */
  public void createRefundGatewayExceptionKeepsPendingForRetry() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2);
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
    when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
        .thenThrow(new RuntimeException("gateway timeout"));

    ArgumentCaptor<PayRefundPo> refundCaptor = ArgumentCaptor.forClass(PayRefundPo.class);
    when(payRefundManager.save(refundCaptor.capture())).thenReturn(true);

    assertThrows(
        BizException.class,
        () ->
            service.createRefund(
                new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
                    1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1")));

    assertEquals(0, refundCaptor.getValue().getStatus());
    verify(payRefundManager, never()).updateStatusIf(anyString(), anyInt(), anyInt(), anyString());
    verify(manager, never()).updateStatusIf("PAY123", 5, 2);
    verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDto.class));
  }

  /**
   * 验证待退款扫描可恢复渠道已成功的退款。
   * 抢占待处理记录后查询到渠道成功，应标记退款和支付状态、通知订单，且不重复发起退款。
   */
  public void scanPendingRefundsCompletesRefundFoundInGateway() {
    PayRefundPo pending = new PayRefundPo();
    pending.setRefundNo("REFUND-STABLE");
    pending.setPaymentNo("PAY123");
    pending.setAmount(new BigDecimal("50.00"));
    pending.setStatus(0);
    pending.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
    when(payRefundManager.listPendingRefunds(any(LocalDateTime.class), eq(100)))
        .thenReturn(List.of(pending));
    when(payRefundManager.claimPendingRefund(
            eq("REFUND-STABLE"), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(1);

    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(payTx);
    when(payGateway.queryRefund("PAY123", "REFUND-STABLE"))
        .thenReturn(PayGateway.RefundQueryStatus.SUCCESS);
    when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
    when(payRefundManager.updateStatusIf(eq("REFUND-STABLE"), eq(0), eq(1), any())).thenReturn(1);

    int count = service.scanPendingRefunds();

    assertEquals(1, count);
    verify(payGateway, never())
        .refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    verify(manager).updateStatusIf("PAY123", 5, 2);
    ArgumentCaptor<RefundCallbackDto> callback = ArgumentCaptor.forClass(RefundCallbackDto.class);
    verify(payRemoteGateway).refundCallback(callback.capture());
    assertEquals(false, callback.getValue().getFullyRefunded());
  }

  /**
   * 验证累计退款达到支付金额时支付单转为已退款。
   * 全额成功退款后，支付状态应通过条件更新从 5 变为 6，订单回调标记全额退款。
   */
  public void createRefundFullRefundMarksRefunded() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.getOne(any())).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123"))
        .thenReturn(new BigDecimal("0.00"))
        .thenReturn(new BigDecimal("100.00"));
    when(payRefundManager.updateStatusIf(anyString(), eq(0), eq(1), anyString())).thenReturn(1);
    when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
        .thenReturn("REFUND_TRADE_123");

    com.yirancrazy.minimall.api.dto.pay.RefundCreateDto dto =
        new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
            1L, new BigDecimal("100.00"), "reason");
    service.createRefund(dto);

    verify(manager).updateStatusIf("PAY123", 5, 6); // 累计退满置 REFUNDED
    ArgumentCaptor<RefundCallbackDto> callback = ArgumentCaptor.forClass(RefundCallbackDto.class);
    verify(payRemoteGateway).refundCallback(callback.capture());
    assertEquals(true, callback.getValue().getFullyRefunded());
  }

  /**
   * 验证退款成功后订单通知失败不回滚资金结果。
   * 退款状态应保持成功、支付单未退满时恢复为成功，且不写入失败退款状态。
   */
  public void createRefundGatewaySuccessOrderCallbackFailsKeepsSuccess() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2);
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.getOne(any())).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123"))
        .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
    when(payRefundManager.updateStatusIf(anyString(), eq(0), eq(1), anyString())).thenReturn(1);
    when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
        .thenReturn("REFUND_TRADE_123");
    when(payRemoteGateway.refundCallback(any(RefundCallbackDto.class)))
        .thenThrow(new RuntimeException("order unavailable"));

    RefundVo vo =
        service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
                1L, new BigDecimal("50.00"), "reason"));

    assertEquals(1, vo.getStatus());
    verify(manager).updateStatusIf("PAY123", 5, 2);
    verify(payRefundManager, never()).updateById(argThat((PayRefundPo r) -> r.getStatus() == 2));
  }

  /**
   * 验证未通知退款扫描会补发订单回调并记录通知时间。
   * 订单回调成功后，处理计数为 1，退款 `notifiedAt` 非空并持久化更新。
   */
  public void scanUnnotifiedRefundsRetriesAndMarksNotified() {
    PayRefundPo refund = new PayRefundPo();
    refund.setRefundNo("R100");
    refund.setPaymentNo("PAY123");
    refund.setStatus(1);
    refund.setCreateTime(LocalDateTime.now());
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("100");
    when(payRefundManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(List.of(refund));
    when(manager.getOne(any())).thenReturn(payTx);
    when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
    when(payRemoteGateway.refundCallback(any(RefundCallbackDto.class)))
        .thenReturn(Result.success(null));
    when(payRefundManager.updateById(refund)).thenReturn(true);

    int count = service.scanUnnotifiedRefunds();

    assertEquals(1, count);
    assertNotNull(refund.getNotifiedAt());
    verify(payRefundManager).updateById(refund);
  }

  /**
   * 验证已支付未推进扫描调用数字订单推进接口。
   * 成功支付流水订单号为 `100` 时，扫描应调用 `pay(100)` 并计数 1。
   */
  public void scanPaidButOrderPendingCallsPayWhenPending() {
    PayTransactionPo tx = new PayTransactionPo();
    tx.setPaymentNo("PAY1");
    tx.setOrderNo("100");
    tx.setStatus(2);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.List.of(tx));
    int count = service.scanPaidButOrderPending();

    assertEquals(1, count);
    verify(payRemoteGateway).pay(100L);
  }

  /**
   * 验证数字订单号在补偿扫描中解析为 Long。
   * 成功流水订单号 `100` 应触发 `pay(100L)`，而不是按字符串订单号接口处理。
   */
  public void scanPaidButOrderPendingAdvancesNumericOrder() {
    PayTransactionPo tx = new PayTransactionPo();
    tx.setPaymentNo("PAY1");
    tx.setOrderNo("100");
    tx.setStatus(2);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.List.of(tx));
    int count = service.scanPaidButOrderPending();

    assertEquals(1, count);
    verify(payRemoteGateway).pay(100L);
  }

  /**
   * 验证补偿扫描同时支持数字和非数字订单号。
   * 两笔成功流水应分别调用 `pay(200)` 与 `payByOrderNo("NOT_AN_ID")`，计数为 2。
   */
  public void scanPaidButOrderPendingSupportsNonNumericOrderNo() {
    PayTransactionPo tx1 = new PayTransactionPo();
    tx1.setPaymentNo("PAY1");
    tx1.setOrderNo("NOT_AN_ID");
    tx1.setStatus(2);
    PayTransactionPo tx2 = new PayTransactionPo();
    tx2.setPaymentNo("PAY2");
    tx2.setOrderNo("200");
    tx2.setStatus(2);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.List.of(tx1, tx2));
    int count = service.scanPaidButOrderPending();

    assertEquals(2, count);
    verify(payRemoteGateway).payByOrderNo("NOT_AN_ID");
    verify(payRemoteGateway).pay(200L);
  }

  /**
   * 验证没有待补偿支付流水时扫描返回 0。
   * Manager 返回空列表后，不应调用远程订单服务。
   */
  public void scanPaidButOrderPendingReturnsZeroWhenEmpty() {
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.Collections.emptyList());

    assertEquals(0, service.scanPaidButOrderPending());
  }

  /**
   * 验证订单快照商家为空时支付流水商家落为 0。
   * 以有效订单创建支付后，捕获实体的 `merchantId` 应等于 0。
   */
  public void createPaymentNullMerchantDefaultsToZero() {
    Long id = service.createPayment("ORDER300", 3L, null, new BigDecimal("10.00"), null);
    assertNotNull(id);
    ArgumentCaptor<PayTransactionPo> cap = ArgumentCaptor.forClass(PayTransactionPo.class);
    verify(manager).save(cap.capture());
    assertEquals(0L, cap.getValue().getMerchantId());
  }

  /**
   * 验证创建支付时商家归属以订单快照为准。
   * 请求未传商家但快照商家为 42 时，支付流水应保存 42。
   */
  public void createPaymentResolvesMerchantFromOrder() {
    Long id = service.createPayment("ORDER400", 4L, null, new BigDecimal("10.00"), null);

    assertNotNull(id);
    ArgumentCaptor<PayTransactionPo> cap = ArgumentCaptor.forClass(PayTransactionPo.class);
    verify(manager).save(cap.capture());
    assertEquals(42L, cap.getValue().getMerchantId());
  }

  /**
   * 验证显式支付宝渠道被持久化并创建待支付流水。
   * 渠道 code 1 时，保存实体的渠道应为 1、状态应为 1。
   */
  public void createPaymentExplicitAlipayChannelPersists() {
    Long id = service.createPayment("ORDER200", 2L, 3L, new BigDecimal("50.00"), 1);
    assertNotNull(id);
    ArgumentCaptor<PayTransactionPo> cap = ArgumentCaptor.forClass(PayTransactionPo.class);
    verify(manager).save(cap.capture());
    assertEquals(1, cap.getValue().getChannel());
    assertEquals(1, cap.getValue().getStatus());
  }

  private static OrderPaySnapshotDto snapshot(
      String orderNo, Long userId, Long merchantId, String amount, int status) {
    return new OrderPaySnapshotDto(
        1L, orderNo, userId, merchantId, new BigDecimal(amount), status, null);
  }

  /**
   * 验证失败回调即使订单号为空也只更新本地失败状态。
   * 待支付流水收到失败结果后应变为 3，且不调用任何订单推进接口。
   */
  public void handleCallbackFailedWithNullOrderNoSkipsFeign() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(1);
    rec.setOrderNo(null);
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY1", "TRADE1", false, "fail-response");
    service.handleCallback(dto);

    assertEquals(3, rec.getStatus());
    verify(payRemoteGateway, never()).pay(any());
  }

  /**
   * 验证成功回调金额不一致时冻结支付单并停止订单推进。
   * 库内金额 100、回调金额 `99.99` 时，状态应变为 7 并更新实体，不调用订单服务。
   */
  public void handleCallbackAmountMismatchFreezesWithoutAdvancingOrder() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(1);
    rec.setOrderNo("100");
    rec.setAmount(new BigDecimal("100.00"));
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto =
        new PayCallbackDto("PAY123", "TRADE123", true, "response", new BigDecimal("99.99"));
    service.handleCallback(dto);

    assertEquals(7, rec.getStatus()); // 已冻结（FROZEN）
    verify(manager).updateById(rec);
    verify(payRemoteGateway, never()).pay(anyLong());
    verify(payRemoteGateway, never()).payByOrderNo(anyString());
  }

  /**
   * 验证处理回调时订单远程异常会抛出且本地状态未被测试替身修改。
   * 订单推进抛异常后应抛出业务异常，当前实体仍为待支付且不执行更新。
   */
  public void handleCallbackOrderAdvanceFailureKeepsPending() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(1);
    rec.setOrderNo("100");
    rec.setAmount(new BigDecimal("100.00"));
    when(manager.getOne(any())).thenReturn(rec);
    when(payRemoteGateway.pay(100L)).thenThrow(new RuntimeException("order unavailable"));

    PayCallbackDto dto =
        new PayCallbackDto("PAY123", "TRADE123", true, "response", new BigDecimal("100.00"));

    assertThrows(BizException.class, () -> service.handleCallback(dto));
    assertEquals(1, rec.getStatus());
    verify(manager, never()).updateById(rec);
  }

  /**
   * 验证重复成功回调保持幂等。
   * 已成功流水再次收到成功回调时，不应更新实体或重复推进订单。
   */
  public void handleCallbackAlreadySuccessIgnoresDuplicate() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(2); // 支付成功（SUCCESS）
    rec.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", true, "response");
    service.handleCallback(dto);

    assertEquals(2, rec.getStatus());
    verify(manager, never()).updateById(any(PayTransactionPo.class));
    verify(payRemoteGateway, never()).pay(anyLong());
  }

  /**
   * 验证失败回调不能覆盖已成功支付。
   * 成功流水收到失败结果后应保持状态 2，不更新实体，也不推进订单。
   */
  public void handleCallbackFailureDoesNotOverwriteSuccess() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(2); // 支付成功（SUCCESS）
    rec.setOrderNo("100");
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", false, "fail-response");
    service.handleCallback(dto);

    assertEquals(2, rec.getStatus());
    verify(manager, never()).updateById(any(PayTransactionPo.class));
    verify(payRemoteGateway, never()).pay(anyLong());
  }

  /**
   * 验证失败回调只从待支付状态生效。
   * 已失败流水再次收到失败回调时应保持 3，且不重复更新。
   */
  public void handleCallbackFailureIgnoredWhenNotPending() {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setId(1L);
    rec.setStatus(3); // 支付失败（FAILED）
    when(manager.getOne(any())).thenReturn(rec);

    PayCallbackDto dto = new PayCallbackDto("PAY123", "TRADE123", false, "fail-response");
    service.handleCallback(dto);

    assertEquals(3, rec.getStatus());
    verify(manager, never()).updateById(any(PayTransactionPo.class));
  }

  /**
   * 验证非数字订单号的退款成功后跳过订单退款回调。
   * 退款本身应成功，支付单未退满时恢复为成功，但不调用订单服务。
   */
  public void createRefundNonNumericOrderNoSkipsFeign() {
    PayTransactionPo payTx = new PayTransactionPo();
    payTx.setId(1L);
    payTx.setStatus(2); // 支付成功（SUCCESS）
    payTx.setPaymentNo("PAY123");
    payTx.setAmount(new BigDecimal("100.00"));
    payTx.setOrderNo("OD20260814001");
    when(manager.getById(1L)).thenReturn(payTx);
    when(manager.getOne(any())).thenReturn(payTx);
    when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
    when(payRefundManager.sumCommittedAmount("PAY123"))
        .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
    when(payRefundManager.updateStatusIf(anyString(), eq(0), eq(1), anyString())).thenReturn(1);
    when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
        .thenReturn("REFUND_TRADE_123");

    RefundVo vo =
        service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDto(
                1L, new BigDecimal("50.00"), "reason"));

    assertNotNull(vo);
    verify(payGateway).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDto.class));
    verify(manager).updateStatusIf("PAY123", 5, 2); // 未退满回 SUCCESS
  }

  /**
   * 验证单笔订单推进异常不会中断同批其他支付补偿。
   * 第一笔远程调用抛异常、第二笔成功时，扫描计数仅包含第二笔且两笔均被尝试。
   */
  public void scanPaidButOrderPendingProbeExceptionSkips() {
    PayTransactionPo tx1 = new PayTransactionPo();
    tx1.setPaymentNo("PAY1");
    tx1.setOrderNo("100");
    tx1.setStatus(2);
    PayTransactionPo tx2 = new PayTransactionPo();
    tx2.setPaymentNo("PAY2");
    tx2.setOrderNo("200");
    tx2.setStatus(2);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.List.of(tx1, tx2));
    when(payRemoteGateway.pay(100L)).thenThrow(new RuntimeException("rpc down"));

    int count = service.scanPaidButOrderPending();

    assertEquals(1, count);
    verify(payRemoteGateway).pay(100L);
    verify(payRemoteGateway).pay(200L);
  }

  /**
   * 验证订单服务返回失败时该支付不计入补偿成功数。
   * 单笔流水推进返回错误码时，扫描结果应为 0，同时保留远程调用记录。
   */
  public void scanPaidButOrderPendingAdvanceFailureSkipsCount() {
    PayTransactionPo tx = new PayTransactionPo();
    tx.setPaymentNo("PAY1");
    tx.setOrderNo("100");
    tx.setStatus(2);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.List.of(tx));
    when(payRemoteGateway.pay(100L)).thenReturn(Result.fail("40012", "advance failed"));

    int count = service.scanPaidButOrderPending();

    assertEquals(0, count);
    verify(payRemoteGateway).pay(100L);
  }

  /**
   * 验证已支付未推进扫描按时间窗和 ID 游标分批查询。
   * 首批 100 条后应发起第二批；查询需包含时间上下界、`LIMIT 100`、升序和游标 100。
   */
  public void scanPaidButOrderPendingBatchesWithWindowAndCursor() {
    List<PayTransactionPo> firstBatch = new ArrayList<>(100);
    for (long i = 1; i <= 100; i++) {
      PayTransactionPo tx = new PayTransactionPo();
      tx.setId(i);
      tx.setPaymentNo("PAY" + i);
      tx.setOrderNo(String.valueOf(i));
      tx.setStatus(2);
      firstBatch.add(tx);
    }
    PayTransactionPo last = new PayTransactionPo();
    last.setId(101L);
    last.setPaymentNo("PAY101");
    last.setOrderNo("101");
    last.setStatus(2);
    when(manager.list(any(Wrapper.class))).thenReturn(firstBatch).thenReturn(List.of(last));
    service.scanPaidButOrderPending();

    ArgumentCaptor<Wrapper<PayTransactionPo>> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(manager, times(2)).list(captor.capture());
    LambdaQueryWrapper<PayTransactionPo> first =
        (LambdaQueryWrapper<PayTransactionPo>) captor.getAllValues().get(0);
    LambdaQueryWrapper<PayTransactionPo> second =
        (LambdaQueryWrapper<PayTransactionPo>) captor.getAllValues().get(1);
    String firstSql = first.getSqlSegment();
    String secondSql = second.getSqlSegment();
    // 每批均带 LIMIT 100 分页
    assertTrue(firstSql.contains("LIMIT 100"));
    assertTrue(secondSql.contains("LIMIT 100"));
    // 时间窗：paidAt >= 窗口起点 AND paidAt < 阈值，对应参数各一个 LocalDateTime
    assertTrue(firstSql.contains("paid_at >="));
    assertTrue(firstSql.contains("paid_at <"));
    assertEquals(
        2,
        first.getParamNameValuePairs().values().stream()
            .filter(LocalDateTime.class::isInstance)
            .count());
    // 第二批游标推进为 id > 100，且按 id 升序取批
    assertTrue(secondSql.contains("id >"));
    assertTrue(secondSql.contains("ORDER BY id ASC"));
    assertTrue(second.getParamNameValuePairs().containsValue(100L));
  }

  /**
   * 验证补偿扫描最多处理十个批次。
   * 当每批始终返回 100 条时，Manager 查询应恰好调用十次后停止。
   */
  public void scanPaidButOrderPendingStopsAfterMaxBatches() {
    List<PayTransactionPo> fullBatch = new ArrayList<>(100);
    for (long i = 1; i <= 100; i++) {
      PayTransactionPo tx = new PayTransactionPo();
      tx.setId(i);
      tx.setPaymentNo("PAY" + i);
      tx.setOrderNo(String.valueOf(i));
      tx.setStatus(2);
      fullBatch.add(tx);
    }
    when(manager.list(any(Wrapper.class))).thenReturn(fullBatch);
    service.scanPaidButOrderPending();

    verify(manager, times(10)).list(any(Wrapper.class));
  }

  /**
   * 验证带游标、状态、时间范围和自定义条数的商家分页仍委托 Manager。
   * 完整筛选查询应返回非空游标页，并调用一次 `list`。
   */
  public void pageWithFullFiltersDelegatesToManager() {
    PayPageDto dto = new PayPageDto();
    dto.setCursor("MTAw");
    dto.setStatus(2);
    dto.setStartTime(java.time.LocalDateTime.of(2026, 7, 1, 0, 0));
    dto.setEndTime(java.time.LocalDateTime.of(2026, 7, 31, 23, 59));
    dto.setLimit(20);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(new java.util.ArrayList<>());

    CursorPageVo<PayTransactionPo> result = service.page(10L, dto);

    assertNotNull(result);
    verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 验证无筛选条件导出仍走 Manager 并返回空列表。
   * Manager 返回空集合时，导出结果非 null、大小为 0，并调用一次查询。
   */
  public void exportTransactionsEmptyFiltersReturnsEmpty() {
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(new java.util.ArrayList<>());

    java.util.List<PayTransactionPo> result = service.exportTransactions(new PayPageDto());

    assertNotNull(result);
    assertEquals(0, result.size());
    verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 验证渠道响应可经 AES 加密落库并可逆解密。
   * 同一密钥加密后密文不等于原文，解密结果应恢复完整渠道响应 JSON。
   */
  public void channelResponseIsEncryptedAtRest() {
    String plain = "{\"trade_no\":\"T20260813\",\"amount\":\"9.90\",\"sign\":\"abc\"}";
    byte[] key = "0123456789abcdef0123456789abcdef".getBytes();

    String cipher = com.yirancrazy.minimall.common.security.AesEncryptor.encrypt(plain, key);

    assertNotNull(cipher);
    org.junit.jupiter.api.Assertions.assertNotEquals(plain, cipher);
    assertEquals(plain, com.yirancrazy.minimall.common.security.AesEncryptor.decrypt(cipher, key));
  }

  /** 验证支付宝回调：网关验签异常，预期返回失败确认且不处理流水. */
  @Test
  public void testHandleAlipayCallbackVerifyFailureReturnsFalse() {
    when(payGateway.verifyCallback(any())).thenThrow(new RuntimeException("bad signature"));

    boolean ack =
        service.handleAlipayCallback(
            new AlipayCallbackDto(
                java.util.Map.of("out_trade_no", "PAY1", "trade_status", "TRADE_SUCCESS")));

    org.junit.jupiter.api.Assertions.assertFalse(ack);
    verify(payGateway).verifyCallback(any());
  }

  /** 验证支付宝回调：验签和金额匹配，预期确认成功并推进订单. */
  @Test
  public void testHandleAlipayCallbackSuccessAdvancesOrder() {
    PayTransactionPo po = new PayTransactionPo();
    po.setId(1L);
    po.setPaymentNo("PAY1");
    po.setOrderNo("ORDER100");
    po.setAmount(new BigDecimal("10.00"));
    po.setStatus(1);
    when(manager.getOne(any(Wrapper.class))).thenReturn(po);
    when(payGateway.verifyCallback(any())).thenReturn("T1");

    boolean ack =
        service.handleAlipayCallback(
            new AlipayCallbackDto(
                java.util.Map.of(
                    "out_trade_no",
                    "PAY1",
                    "trade_status",
                    "TRADE_SUCCESS",
                    "total_amount",
                    "10.00")));

    assertTrue(ack);
    verify(payRemoteGateway).payByOrderNo("ORDER100");
  }

  /** 验证支付宝回调：订单推进失败，预期支付仍成功且确认返回成功. */
  @Test
  public void testHandleAlipayCallbackOrderAdvanceFailureKeepsPaymentSuccess() {
    PayTransactionPo po = new PayTransactionPo();
    po.setId(1L);
    po.setPaymentNo("PAY1");
    po.setOrderNo("ORDER100");
    po.setAmount(new BigDecimal("10.00"));
    po.setStatus(Integer.parseInt(PayStatusEnum.PENDING.getCode()));
    when(manager.getOne(any(Wrapper.class))).thenReturn(po);
    when(payGateway.verifyCallback(any())).thenReturn("T1");
    when(payRemoteGateway.payByOrderNo("ORDER100")).thenReturn(Result.fail("20000", "订单服务不可用"));

    boolean ack =
        service.handleAlipayCallback(
            new AlipayCallbackDto(
                java.util.Map.of(
                    "out_trade_no",
                    "PAY1",
                    "trade_status",
                    "TRADE_SUCCESS",
                    "total_amount",
                    "10.00")));

    assertTrue(ack);
    assertEquals(Integer.parseInt(PayStatusEnum.SUCCESS.getCode()), po.getStatus());
    assertEquals("T1", po.getTradeNo());
    assertNotNull(po.getPaidAt());
    var ordered = inOrder(manager, payRemoteGateway);
    ordered
        .verify(manager)
        .markSuccessIfPending(
            eq("PAY1"),
            eq("T1"),
            anyString(),
            any(LocalDateTime.class),
            eq(Integer.parseInt(PayStatusEnum.PENDING.getCode())),
            eq(Integer.parseInt(PayStatusEnum.SUCCESS.getCode())));
    ordered.verify(payRemoteGateway).payByOrderNo("ORDER100");
  }

  /** 验证非待支付单不能生成收银台参数. */
  @Test
  public void testGetPaymentParamsRejectsClosedPayment() {
    PayTransactionPo rec = paymentForParams(PayStatusEnum.CLOSED);
    when(manager.getOne(any())).thenReturn(rec);

    BizException ex =
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY123", 1L));

    assertEquals(PayCodeEnum.PAY_NOT_PENDING.getCode(), ex.getCode());
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /** 验证过期待支付单不能生成收银台参数. */
  @Test
  public void testGetPaymentParamsRejectsExpiredPendingPayment() {
    PayTransactionPo rec = paymentForParams(PayStatusEnum.PENDING);
    rec.setExpireAt(LocalDateTime.now().minusSeconds(1));
    when(manager.getOne(any())).thenReturn(rec);

    BizException ex =
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY123", 1L));

    assertEquals(PayCodeEnum.PAY_NOT_PENDING.getCode(), ex.getCode());
    verify(payGateway, never())
        .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
  }

  /** 验证待支付单缺少活跃订单号时不能生成收银台参数. */
  @Test
  public void testGetPaymentParamsRejectsMissingActiveOrderNo() {
    PayTransactionPo rec = paymentForParams(PayStatusEnum.PENDING);
    rec.setActiveOrderNo(null);
    when(manager.getOne(any())).thenReturn(rec);

    BizException ex =
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY123", 1L));

    assertEquals(PayCodeEnum.PAY_NOT_PENDING.getCode(), ex.getCode());
  }

  /** 验证关单仅使用待支付到已关闭的条件更新. */
  @Test
  public void testClosePaymentUsesConditionalUpdate() {
    PayTransactionPo rec = paymentForParams(PayStatusEnum.PENDING);
    when(manager.getOne(any())).thenReturn(rec);
    when(payGateway.closePayment("PAY123")).thenReturn("TRADE_CLOSED");
    when(
            manager.closePendingPayment(
                "PAY123",
                Integer.parseInt(PayStatusEnum.PENDING.getCode()),
                Integer.parseInt(PayStatusEnum.CLOSED.getCode())))
        .thenReturn(1);

    String result = service.closePayment("PAY123");

    assertEquals("TRADE_CLOSED", result);
    verify(manager)
        .closePendingPayment(
            "PAY123",
            Integer.parseInt(PayStatusEnum.PENDING.getCode()),
            Integer.parseInt(PayStatusEnum.CLOSED.getCode()));
    verify(manager, never()).updateById(any(PayTransactionPo.class));
  }

  /** 验证关单条件更新冲突且状态已变化时拒绝覆盖. */
  @Test
  public void testClosePaymentRejectsConcurrentStatusChange() {
    PayTransactionPo pending = paymentForParams(PayStatusEnum.PENDING);
    PayTransactionPo success = paymentForParams(PayStatusEnum.SUCCESS);
    when(manager.getOne(any())).thenReturn(pending, success);
    when(payGateway.closePayment("PAY123")).thenReturn("TRADE_CLOSED");
    when(
            manager.closePendingPayment(
                "PAY123",
                Integer.parseInt(PayStatusEnum.PENDING.getCode()),
                Integer.parseInt(PayStatusEnum.CLOSED.getCode())))
        .thenReturn(0);

    BizException ex = assertThrows(BizException.class, () -> service.closePayment("PAY123"));

    assertEquals(PayCodeEnum.PAY_CLOSE_STATUS_INVALID.getCode(), ex.getCode());
  }

  /** 验证已关闭支付单收到迟到成功回调时不推进订单. */
  @Test
  public void testClosedPaymentCallbackDoesNotAdvanceOrder() {
    PayTransactionPo rec = paymentForParams(PayStatusEnum.CLOSED);
    when(manager.getOne(any())).thenReturn(rec);
    when(
            manager.markSuccessIfPending(
                anyString(), anyString(), anyString(), any(LocalDateTime.class), anyInt(), anyInt()))
        .thenReturn(0);

    service.handleCallback(new PayCallbackDto("PAY123", "TRADE123", true, "response", null));

    verify(payRemoteGateway, never()).pay(anyLong());
    verify(payRemoteGateway, never()).payByOrderNo(anyString());
    assertEquals(Integer.parseInt(PayStatusEnum.CLOSED.getCode()), rec.getStatus());
  }

  private PayTransactionPo paymentForParams(PayStatusEnum status) {
    PayTransactionPo rec = new PayTransactionPo();
    rec.setPaymentNo("PAY123");
    rec.setOrderNo("ORDER100");
    rec.setUserId(1L);
    rec.setAmount(new BigDecimal("99.99"));
    rec.setCurrency("CNY");
    rec.setChannel(1);
    rec.setStatus(Integer.parseInt(status.getCode()));
    rec.setActiveOrderNo("ORDER100");
    rec.setExpireAt(LocalDateTime.now().plusMinutes(15));
    return rec;
  }
}
