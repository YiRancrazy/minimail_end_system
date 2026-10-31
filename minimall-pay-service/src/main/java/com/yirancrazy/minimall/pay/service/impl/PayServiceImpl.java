package com.yirancrazy.minimall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
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
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVo;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVo;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVo;
import com.yirancrazy.minimall.pay.vo.RefundVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现支付创建、渠道回调、退款编排、查询统计和补偿扫描。
 * 本地支付与退款流水经 Manager 持久化，支付宝和订单交互经 Gateway 隔离，关键状态写入使用独立事务。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class PayServiceImpl implements PayService {

  private static final String PAYMENT_NO_PREFIX = "PAY";
  private static final DateTimeFormatter EXPIRE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final int PAY_CALLBACK_RPC_BUFFER_SECONDS = 30;
  private static final int ORDER_PENDING_CODE = 1;
  // 金额统一使用元并保留两位小数；更高精度尾数一律拒绝，避免静默进位
  private static final int AMOUNT_SCALE = 2;
  // 对账补偿扫描分批参数：单批 100 条、单次最多 10 批，仅扫描近 24h 成功流水，限制单次调度扫描量与耗时
  private static final int SCAN_BATCH_SIZE = 100;
  private static final int SCAN_MAX_BATCHES = 10;
  private static final long SCAN_WINDOW_HOURS = 24L;
  private static final long REFUND_RETRY_DELAY_SECONDS = 30L;
  private static final long REFUND_PROCESSING_LEASE_SECONDS = 60L;

  private final PayManager payManager;
  private final PayRefundManager payRefundManager;
  private final PayGateway payGateway;
  private final PayRemoteGateway payRemoteGateway;
  private final TransactionTemplate transactionTemplate;

  /**
   * 注入支付数据访问、渠道网关和远程服务依赖，并创建独立事务模板。
   * 事务模板使用 `REQUIRES_NEW`，用于退款额度预留等需要独立提交或回滚的状态变更。
   *
   * @param payManager 支付交易数据访问入口
   * @param payRefundManager 退款流水数据访问入口
   * @param payGateway 支付宝页面支付、验签和退款网关
   * @param payRemoteGateway ID 与订单远程服务网关
   * @param transactionManager 平台事务管理器
   */
  public PayServiceImpl(
      PayManager payManager,
      PayRefundManager payRefundManager,
      PayGateway payGateway,
      PayRemoteGateway payRemoteGateway,
      PlatformTransactionManager transactionManager) {
    this.payManager = payManager;
    this.payRefundManager = payRefundManager;
    this.payGateway = payGateway;
    this.payRemoteGateway = payRemoteGateway;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * 创建或幂等复用当前用户的支付单。
   * 以订单快照校验用户、应付金额和待支付状态；成功或未过期的待支付单直接复用，
   * 过期待支付单关闭后重建，并由活跃订单唯一约束兜底并发创建。
   * 本方法只落本地流水，不同步调用支付宝生成表单。
   *
   * @param orderNo 业务订单号
   * @param userId 支付发起用户
   * @param merchantId 调用方提示的商家 ID，最终以订单快照为准
   * @param amount 支付金额，最多两位小数
   * @param channel 支付渠道 code，为空时默认支付宝
   * @return 新建或复用的支付单主键
   */
  @Override
  public Long createPayment(
      String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel) {
    int alipayCode = Integer.parseInt(PayChannelEnum.ALIPAY.getCode()); // 获取支付宝渠道编码（当前仅支持支付宝）
    int channelCode = channel == null ? alipayCode : channel; // 渠道为空时默认使用支付宝

    // 仅允许支付宝渠道，其他渠道直接抛出不支持异常
    if (channelCode != alipayCode) {
      throw new BizException(PayCodeEnum.PAY_CHANNEL_UNSUPPORTED);
    }

    // 订单快照是支付归属、金额和状态校验的唯一可信来源，客户端 merchantId 不参与决策
    OrderPaySnapshotDto snapshot = loadPaySnapshot(orderNo);
    validatePaymentRequest(snapshot, userId, amount);
    amount = normalizeAmount(amount);
    merchantId = snapshot.getMerchantId() == null ? 0L : snapshot.getMerchantId();

    // 根据订单号查询已有支付交易记录，用于幂等判断
    PayTransactionPo existing = latestByOrderNo(orderNo);

    // 幂等复用：成功单始终复用；待支付单仅未过期时复用，过期 PENDING 视为失效走新建，
    // 避免残留待支付单被后续请求静默复用导致支付流程无响应也无日志
    if (existing != null) {
      int status = existing.getStatus();
      boolean pendingAlive =
          status == Integer.parseInt(PayStatusEnum.PENDING.getCode())
              && existing.getExpireAt() != null
              && existing.getExpireAt().isAfter(LocalDateTime.now());

      // 成功单或未过期的待支付单可复用
      if (status == Integer.parseInt(PayStatusEnum.SUCCESS.getCode()) || pendingAlive) {
        log.info(
            "支付单已复用，paymentNo={}，orderNo={}，status={}", existing.getPaymentNo(), orderNo, status);
        return existing.getId();
      }

      // 过期待支付单视作无效，关闭并释放订单号
      if (status == Integer.parseInt(PayStatusEnum.PENDING.getCode())) {
        log.warn(
            "支付单已过期但仍待支付，创建新支付单，paymentNo={}，orderNo={}，expireAt={}",
            existing.getPaymentNo(),
            orderNo,
            existing.getExpireAt());
        existing.setStatus(Integer.parseInt(PayStatusEnum.CLOSED.getCode()));
        existing.setActiveOrderNo(null);
        payManager.updateById(existing);
      }
    }

    // 订单状态校验
    if (snapshot.getStatus() == null || snapshot.getStatus() != ORDER_PENDING_CODE) {
      throw new BizException(PayCodeEnum.PAY_ORDER_STATUS_INVALID);
    }

    // 生成支付流水号
    String paymentNo = generatePaymentNo();

    // 支付过期时间设置为当前时间15分钟后
    LocalDateTime expireAt = LocalDateTime.now().plusMinutes(15);

    // 组装新的支付交易记录
    PayTransactionPo po = new PayTransactionPo();
    po.setPaymentNo(paymentNo);
    po.setOrderNo(orderNo);
    po.setActiveOrderNo(orderNo);
    po.setUserId(userId);
    po.setMerchantId(merchantId);
    po.setAmount(amount);
    po.setCurrency("CNY");
    po.setStatus(Integer.parseInt(PayStatusEnum.PENDING.getCode()));
    po.setChannel(channelCode);
    po.setExpireAt(expireAt);
    po.setIdempotencyKey(UUID.randomUUID().toString());

    try {
      payManager.save(po);
    } catch (DuplicateKeyException e) {
      // 活跃订单唯一键兜底并发创建：返回已存在的活跃支付单，避免同订单多张可支付流水
      PayTransactionPo active = latestByOrderNo(orderNo);
      if (active != null
          && active.getStatus() != null
          && active.getStatus() != Integer.parseInt(PayStatusEnum.FAILED.getCode())
          && active.getStatus() != Integer.parseInt(PayStatusEnum.CLOSED.getCode())) {
        log.warn("重复创建有效支付单，复用已有支付单，orderNo={}，paymentNo={}", orderNo, active.getPaymentNo());
        return active.getId();
      }
      throw new BizException(PayCodeEnum.PAY_CREATE_CONFLICT);
    }

    // 收银台表单由 getPaymentParams 重新生成，此处不做同步外部调用，避免支付宝不可达时阻塞下单请求
    log.info("支付单已创建，paymentNo={}，orderNo={}，channel={}", paymentNo, orderNo, channelCode);
    return po.getId();
  }

  /**
   * 验签支付宝异步通知并归一化为统一支付回调。
   * 成功或关闭状态会传入状态处理；验签、参数或业务处理异常均返回 false，避免错误确认通知。
   *
   * @param dto 支付宝原始回调参数
   * @return 回调成功受理时返回 true，否则返回 false
   */
  @Override
  public boolean handleAlipayCallback(AlipayCallbackDto dto) {
    Map<String, String> params = dto.getParams();
    try {
      String tradeNo = payGateway.verifyCallback(params);
      String paymentNo = dto.getOutTradeNo();
      String tradeStatus = dto.getTradeStatus();
      boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

      PayCallbackDto callback =
          new PayCallbackDto(
              paymentNo, tradeNo, success, params.toString(), parseTotalAmount(paymentNo, params));
      handleCallback(callback);
      log.info("支付宝回调已处理，paymentNo={}，tradeNo={}，success={}", paymentNo, tradeNo, success);
      return true;
    } catch (BizException e) {
      log.error("回调处理失败: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("回调验签失败", e);
      return false;
    }
  }

  /**
   * 处理支付渠道回调并更新本地支付状态。
   * 成功单直接幂等返回；金额不一致时冻结支付单且不推进订单；成功回调先落支付状态，
   * 订单推进失败留给补偿扫描重试；失败回调只允许从待支付转为失败。
   *
   * @param dto 归一化支付回调
   */
  public void handleCallback(PayCallbackDto dto) {
    // 根据支付流水号查询支付单
    PayTransactionPo po =
        payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPo.class)
                .eq(PayTransactionPo::getPaymentNo, dto.getPaymentNo()));
    if (po == null) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }

    // 幂等：支付单已 SUCCESS 时直接返回，避免重复推进订单与重复回调 order 服务
    if (po.getStatus() == Integer.parseInt(PayStatusEnum.SUCCESS.getCode())) {
      log.info("回调已忽略，支付单已成功，paymentNo={}", dto.getPaymentNo());
      return;
    }

    // 金额校验：回调金额与库内支付单不符时冻结并告警，返回 success 避免支付宝重复投递
    if (dto.getTotalAmount() != null && !matchesAmount(dto.getTotalAmount(), po.getAmount())) {
      po.setStatus(Integer.parseInt(PayStatusEnum.FROZEN.getCode()));
      po.setChannelResponse(dto.getChannelResponse());
      payManager.updateById(po);
      log.error(
          "回调金额不匹配，支付单已冻结，paymentNo={}，callbackAmount={}，dbAmount={}",
          dto.getPaymentNo(),
          dto.getTotalAmount(),
          po.getAmount());
      return;
    }

    if (dto.isSuccess()) {
      applyCallbackSuccess(po, dto);
    } else {
      applyCallbackFailure(po, dto);
    }
    log.info("支付回调已处理，paymentNo={}，success={}", dto.getPaymentNo(), dto.isSuccess());
  }

  private BigDecimal parseTotalAmount(String paymentNo, Map<String, String> params) {
    String rawAmount = params.get("total_amount");
    if (rawAmount == null) {
      return null;
    }
    try {
      return new BigDecimal(rawAmount);
    } catch (NumberFormatException e) {
      log.warn("回调 total_amount 非法，paymentNo={}，totalAmount={}", paymentNo, rawAmount);
      return null;
    }
  }

  private void applyCallbackSuccess(PayTransactionPo po, PayCallbackDto dto) {
    int status = po.getStatus();
    if (status == Integer.parseInt(PayStatusEnum.REFUNDING.getCode())
        || status == Integer.parseInt(PayStatusEnum.REFUNDED.getCode())
        || status == Integer.parseInt(PayStatusEnum.FROZEN.getCode())) {
      log.warn("成功回调已忽略，支付单不处于可支付状态，paymentNo={}，status={}", dto.getPaymentNo(), status);
      return;
    }

    po.setTradeNo(dto.getTradeNo());
    po.setStatus(Integer.parseInt(PayStatusEnum.SUCCESS.getCode()));
    po.setChannelResponse(dto.getChannelResponse());
    po.setPaidAt(LocalDateTime.now());
    payManager.updateById(po);

    // 支付结果先落库，订单推进失败由 scanPaidButOrderPending 重试，避免已收款但支付单长期待支付
    Result<Void> advanceResult = advanceOrder(po.getOrderNo());
    if (advanceResult == null || !CommonCode.SUCCESS.equals(advanceResult.getCode())) {
      log.warn("支付已成功但订单推进失败，等待补偿，paymentNo={}，orderNo={}", po.getPaymentNo(), po.getOrderNo());
    }
  }

  private Result<Void> advanceOrder(String orderNo) {
    if (orderNo == null || orderNo.isBlank()) {
      return Result.fail(
          PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getCode(),
          PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getMessage());
    }
    try {
      return orderNo.matches("\\d+")
          ? payRemoteGateway.pay(Long.valueOf(orderNo))
          : payRemoteGateway.payByOrderNo(orderNo);
    } catch (Exception e) {
      log.warn("推进订单失败，orderNo={}", orderNo, e);
      return Result.fail(
          PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getCode(),
          PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getMessage());
    }
  }

  private void applyCallbackFailure(PayTransactionPo po, PayCallbackDto dto) {
    int status = po.getStatus();
    if (status != Integer.parseInt(PayStatusEnum.PENDING.getCode())) {
      log.warn("失败回调已忽略，支付单不处于待支付状态，paymentNo={}，status={}", dto.getPaymentNo(), status);
      return;
    }
    po.setStatus(Integer.parseInt(PayStatusEnum.FAILED.getCode()));
    po.setActiveOrderNo(null);
    po.setChannelResponse(dto.getChannelResponse());
    payManager.updateById(po);
  }

  private String generatePaymentNo() {
    return PAYMENT_NO_PREFIX + requireGlobalId("PAY");
  }

  private static BigDecimal normalizeAmount(BigDecimal amount) {
    try {
      return amount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new BizException(PayCodeEnum.PAY_AMOUNT_SCALE_INVALID);
    }
  }

  private static boolean matchesAmount(BigDecimal callbackAmount, BigDecimal storedAmount) {
    try {
      return callbackAmount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY).compareTo(storedAmount)
          == 0;
    } catch (ArithmeticException e) {
      return false;
    }
  }

  /**
   * 幂等创建或恢复支付退款。
   * 先复用最终状态或匹配的待处理退款；新退款在独立事务内校验累计金额并原子占用支付状态，
   * 渠道成功后完成退款并通知订单，渠道异常则保留待处理记录供扫描重试。
   *
   * @param dto 支付单、退款金额、原因和可选退款请求号
   * @return 复用、恢复或新完成的退款结果
   */
  @Override
  public RefundVo createRefund(RefundCreateDto dto) {
    PayRefundPo existingRefund = findByRefundRequestNo(dto.getRefundRequestNo());
    if (existingRefund != null && isFinalRefundStatus(existingRefund.getStatus())) {
      log.info(
          "退款请求已复用，requestNo={}，refundNo={}，status={}",
          dto.getRefundRequestNo(),
          existingRefund.getRefundNo(),
          existingRefund.getStatus());
      return toRefundVo(existingRefund);
    }

    PayTransactionPo payTx = payManager.getById(dto.getPayId());
    if (payTx == null) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }
    BigDecimal refundAmount = normalizeAmount(dto.getAmount());
    if (existingRefund != null) {
      validateExistingRefund(existingRefund, payTx, refundAmount);
      return recoverPendingRefund(existingRefund);
    }
    if (payTx.getStatus() != Integer.parseInt(PayStatusEnum.SUCCESS.getCode())) {
      throw new BizException(PayCodeEnum.PAY_NOT_SUCCESS);
    }
    if (refundAmount.compareTo(payTx.getAmount()) > 0) {
      throw new BizException(PayCodeEnum.REFUND_AMOUNT_EXCEED);
    }

    String refundNo = generateRefundNo();
    String refundRequestNo = dto.getRefundRequestNo();
    if (refundRequestNo == null || refundRequestNo.isBlank()) {
      refundRequestNo = refundNo;
    }
    try {
      existingRefund =
          reserveRefund(payTx, refundAmount, dto.getReason(), refundNo, refundRequestNo);
    } catch (DuplicateKeyException e) {
      existingRefund = findByRefundRequestNo(refundRequestNo);
      if (existingRefund == null) {
        throw e;
      }
      validateExistingRefund(existingRefund, payTx, refundAmount);
      return recoverPendingRefund(existingRefund);
    }
    if (existingRefund == null) {
      throw new BizException(PayCodeEnum.REFUND_FAILED);
    }
    return submitRefund(existingRefund);
  }

  private PayRefundPo reserveRefund(
      PayTransactionPo payTx,
      BigDecimal refundAmount,
      String reason,
      String refundNo,
      String refundRequestNo) {
    return transactionTemplate.execute(
        status -> {
          BigDecimal committedAmount = payRefundManager.sumCommittedAmount(payTx.getPaymentNo());
          if (committedAmount.add(refundAmount).compareTo(payTx.getAmount()) > 0) {
            throw new BizException(PayCodeEnum.REFUND_EXCEED);
          }

          PayRefundPo refund = new PayRefundPo();
          refund.setRefundNo(refundNo);
          refund.setRefundRequestNo(refundRequestNo);
          refund.setPaymentNo(payTx.getPaymentNo());
          refund.setAmount(refundAmount);
          refund.setReason(reason);
          refund.setStatus(Integer.parseInt(RefundStatusEnum.PENDING.getCode()));
          refund.setIdempotencyKey(UUID.randomUUID().toString());
          refund.setRetryCount(0);
          refund.setNextRetryAt(LocalDateTime.now().plusSeconds(REFUND_RETRY_DELAY_SECONDS));
          payRefundManager.save(refund);

          int successCode = Integer.parseInt(PayStatusEnum.SUCCESS.getCode());
          int refundingCode = Integer.parseInt(PayStatusEnum.REFUNDING.getCode());
          if (payManager.updateStatusIf(payTx.getPaymentNo(), successCode, refundingCode) == 0) {
            throw new BizException(PayCodeEnum.REFUND_CONFLICT);
          }
          return refund;
        });
  }

  private RefundVo submitRefund(PayRefundPo refund) {
    try {
      String refundTradeNo =
          payGateway.refund(
              refund.getPaymentNo(), refund.getRefundNo(), refund.getAmount(), refund.getReason());
      return completeRefund(refund, refundTradeNo);
    } catch (Exception e) {
      scheduleRefundRetry(refund);
      log.error(
          "退款网关调用失败，refundNo={}，paymentNo={}", refund.getRefundNo(), refund.getPaymentNo(), e);
      throw new BizException(PayCodeEnum.REFUND_FAILED);
    }
  }

  private RefundVo recoverPendingRefund(PayRefundPo refund) {
    PayGateway.RefundQueryStatus queryStatus;
    try {
      queryStatus = payGateway.queryRefund(refund.getPaymentNo(), refund.getRefundNo());
    } catch (Exception e) {
      scheduleRefundRetry(refund);
      log.error("查询退款失败，refundNo={}，paymentNo={}", refund.getRefundNo(), refund.getPaymentNo(), e);
      throw new BizException(PayCodeEnum.REFUND_FAILED);
    }
    if (queryStatus == PayGateway.RefundQueryStatus.SUCCESS) {
      try {
        return completeRefund(refund, refund.getRefundTradeNo());
      } catch (Exception e) {
        scheduleRefundRetry(refund);
        throw new BizException(PayCodeEnum.REFUND_FAILED);
      }
    }
    if (queryStatus == PayGateway.RefundQueryStatus.NOT_FOUND) {
      return submitRefund(refund);
    }
    scheduleRefundRetry(refund);
    log.warn(
        "退款查询仍待处理，refundNo={}，paymentNo={}，status={}",
        refund.getRefundNo(),
        refund.getPaymentNo(),
        queryStatus);
    throw new BizException(PayCodeEnum.REFUND_FAILED);
  }

  private RefundVo completeRefund(PayRefundPo refund, String refundTradeNo) {
    PayTransactionPo payTx =
        payManager.getOne(
            Wrappers.<PayTransactionPo>lambdaQuery()
                .eq(PayTransactionPo::getPaymentNo, refund.getPaymentNo())
                .last("LIMIT 1"));
    BigDecimal totalRefunded = payRefundManager.sumCommittedAmount(refund.getPaymentNo());
    boolean fullyRefunded = false;
    if (payTx != null && payTx.getAmount() != null) {
      fullyRefunded = totalRefunded.compareTo(payTx.getAmount()) >= 0;
      int targetStatus =
          fullyRefunded
              ? Integer.parseInt(PayStatusEnum.REFUNDED.getCode())
              : Integer.parseInt(PayStatusEnum.SUCCESS.getCode());
      payManager.updateStatusIf(
          refund.getPaymentNo(), Integer.parseInt(PayStatusEnum.REFUNDING.getCode()), targetStatus);
    }

    int updated =
        payRefundManager.updateStatusIf(
            refund.getRefundNo(),
            Integer.parseInt(RefundStatusEnum.PENDING.getCode()),
            Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()),
            refundTradeNo);
    if (updated == 0) {
      PayRefundPo current = findByRefundRequestNo(refund.getRefundRequestNo());
      if (current != null
          && current.getStatus() == Integer.parseInt(RefundStatusEnum.SUCCESS.getCode())) {
        return toRefundVo(current);
      }
      throw new BizException(PayCodeEnum.REFUND_CONFLICT);
    }

    refund.setStatus(Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()));
    refund.setRefundTradeNo(refundTradeNo);
    Long orderId = resolveOrderId(payTx);
    if (orderId != null) {
      notifyOrderRefund(orderId, payTx, refund, totalRefunded, fullyRefunded);
    }
    log.info("退款成功，refundNo={}，paymentNo={}", refund.getRefundNo(), refund.getPaymentNo());
    return toRefundVo(refund);
  }

  private void scheduleRefundRetry(PayRefundPo refund) {
    LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(REFUND_RETRY_DELAY_SECONDS);
    refund.setNextRetryAt(nextRetryAt);
    int retryCount = refund.getRetryCount() == null ? 0 : refund.getRetryCount();
    refund.setRetryCount(retryCount + 1);
    try {
      payRefundManager.scheduleRefundRetry(refund.getRefundNo(), nextRetryAt);
    } catch (Exception e) {
      log.error("定时退款重试失败，refundNo={}", refund.getRefundNo(), e);
    }
  }

  private static Long resolveOrderId(PayTransactionPo payTx) {
    if (payTx == null || payTx.getOrderNo() == null) {
      return null;
    }
    try {
      return Long.valueOf(payTx.getOrderNo());
    } catch (NumberFormatException e) {
      log.warn(
          "orderNo 非数字，跳过退款后订单推进，paymentNo={}，orderNo={}",
          payTx.getPaymentNo(),
          payTx.getOrderNo());
      return null;
    }
  }

  private static boolean isFinalRefundStatus(Integer status) {
    return status != null && status != Integer.parseInt(RefundStatusEnum.PENDING.getCode());
  }

  private static void validateExistingRefund(
      PayRefundPo refund, PayTransactionPo payTx, BigDecimal refundAmount) {
    if (!payTx.getPaymentNo().equals(refund.getPaymentNo())
        || refund.getAmount().compareTo(refundAmount) != 0) {
      throw new BizException(PayCodeEnum.REFUND_CONFLICT);
    }
  }

  private PayRefundPo findByRefundRequestNo(String refundRequestNo) {
    if (refundRequestNo == null || refundRequestNo.isBlank()) {
      return null;
    }
    return payRefundManager.findByRefundRequestNo(refundRequestNo);
  }

  private static RefundVo toRefundVo(PayRefundPo refund) {
    return new RefundVo(
        refund.getRefundNo(), refund.getPaymentNo(), refund.getAmount(), refund.getStatus());
  }

  private void notifyOrderRefund(
      Long orderId,
      PayTransactionPo payTx,
      PayRefundPo refund,
      BigDecimal totalRefunded,
      boolean fullyRefunded) {
    try {
      RefundCallbackDto dto =
          new RefundCallbackDto(
              orderId,
              payTx.getId(),
              refund.getAmount(),
              totalRefunded,
              fullyRefunded,
              refund.getRefundRequestNo());
      Result<Void> result = payRemoteGateway.refundCallback(dto);
      if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
        throw new IllegalStateException(
            "订单退款回调被拒绝：" + (result == null ? "null" : result.getCode()));
      }
      refund.setNotifiedAt(LocalDateTime.now());
      payRefundManager.updateById(refund);
    } catch (Exception e) {
      log.error("订单退款回调失败，将重试，orderId={}，refundNo={}", orderId, refund.getRefundNo(), e);
    }
  }

  private String generateRefundNo() {
    return "REFUND" + requireGlobalId("REFUND");
  }

  private Long requireGlobalId(String bizTag) {
    try {
      Result<Long> result = payRemoteGateway.nextId(bizTag);
      if (result != null
          && CommonCode.SUCCESS.equals(result.getCode())
          && result.getData() != null) {
        return result.getData();
      }
      log.error("id-service 返回失败，bizTag={}，result={}", bizTag, result);
    } catch (Exception e) {
      log.error("id-service nextId 失败，bizTag={}", bizTag, e);
    }
    throw new BizException(PayCodeEnum.PAY_ID_GENERATE_FAIL);
  }

  /**
   * 查询当前用户指定订单号的最新支付流水。
   *
   * @param orderNo 业务订单号
   * @param userId 当前用户 ID
   * @return 用户拥有的最新支付流水
   * @throws com.yirancrazy.minimall.common.exception.BizException 支付流水不存在或不属于当前用户
   */
  public PayTransactionPo getByOrderNo(String orderNo, Long userId) {
    PayTransactionPo po = latestByOrderNo(orderNo);
    if (po == null || !ownedBy(po, userId)) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }
    return po;
  }

  /**
   * 将当前用户订单的最新支付流水转换为状态视图。
   *
   * @param orderNo 业务订单号
   * @param userId 当前用户 ID
   * @return 支付状态视图
   */
  @Override
  public PayTransactionVo getStatusVo(String orderNo, Long userId) {
    return PayTransactionVo.from(getByOrderNo(orderNo, userId));
  }

  private static boolean ownedBy(PayTransactionPo po, Long userId) {
    return po.getUserId() != null && po.getUserId().equals(userId);
  }

  private PayTransactionPo latestByOrderNo(String orderNo) {
    return payManager.getOne(
        Wrappers.lambdaQuery(PayTransactionPo.class)
            .eq(PayTransactionPo::getOrderNo, orderNo)
            .orderByDesc(PayTransactionPo::getId)
            .last("LIMIT 1"));
  }

  private OrderPaySnapshotDto loadPaySnapshot(String orderNo) {
    Result<OrderPaySnapshotDto> result;

    // 加载订单支付快照
    try {
      result = payRemoteGateway.paySnapshot(orderNo);
    } catch (Exception e) {
      log.warn("加载订单支付快照失败，orderNo={}", orderNo, e);
      throw new BizException(CommonCode.SYS_ERROR, "订单服务不可用");
    }
    if (result == null
        || !CommonCode.SUCCESS.equals(result.getCode())
        || result.getData() == null) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }
    return result.getData();
  }

  private void validatePaymentRequest(
      OrderPaySnapshotDto snapshot, Long userId, BigDecimal amount) {
    if (snapshot.getUserId() == null || !snapshot.getUserId().equals(userId)) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }
    if (snapshot.getPayAmount() == null
        || amount == null
        || snapshot.getPayAmount().compareTo(amount) != 0) {
      throw new BizException(PayCodeEnum.PAY_AMOUNT_MISMATCH);
    }
  }

  /**
   * 为当前用户支付单实时生成支付宝收银台参数。
   * 方法校验支付单归属，并以数据库金额、支付单号和过期时间生成签名页面表单。
   *
   * @param paymentNo 支付单号
   * @param userId 当前用户 ID
   * @return 收银台主体、金额、渠道、过期时间和支付表单
   */
  @Override
  public PaymentParamsVo getPaymentParams(String paymentNo, Long userId) {
    PayTransactionPo po =
        payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPo.class)
                .eq(PayTransactionPo::getPaymentNo, paymentNo));
    if (po == null || !ownedBy(po, userId)) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }
    String payForm =
        payGateway.createPagePayment(
            po.getPaymentNo(),
            po.getAmount(),
            "Order " + po.getOrderNo(),
            po.getExpireAt().format(EXPIRE_FORMATTER));
    return new PaymentParamsVo(
        po.getPaymentNo(),
        po.getOrderNo(),
        po.getAmount(),
        po.getCurrency(),
        po.getChannel(),
        "Order " + po.getOrderNo(),
        po.getExpireAt(),
        payForm);
  }

  /**
   * 按商家、状态、时间和主键游标分页查询支付流水实体。
   * 查询多取一条记录用于判断是否还有下一页。
   *
   * @param merchantId 商家 ID
   * @param dto 游标、状态和时间筛选条件
   * @return 支付流水实体游标页
   */
  public CursorPageVo<PayTransactionPo> page(Long merchantId, PayPageDto dto) {
    Long lastId = CursorUtils.decode(dto.getCursor());
    int limit = dto.getLimit();
    List<PayTransactionPo> records =
        payManager.list(
            Wrappers.lambdaQuery(PayTransactionPo.class)
                .lt(lastId != null, PayTransactionPo::getId, lastId)
                .eq(PayTransactionPo::getMerchantId, merchantId)
                .eq(dto.getStatus() != null, PayTransactionPo::getStatus, dto.getStatus())
                .ge(dto.getStartTime() != null, PayTransactionPo::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, PayTransactionPo::getCreateTime, dto.getEndTime())
                .orderByDesc(PayTransactionPo::getId)
                .last("LIMIT " + (limit + 1)));
    return CursorPageVo.of(records, limit, PayTransactionPo::getId);
  }

  /**
   * 将商家支付流水游标页转换为对外视图。
   *
   * @param merchantId 商家 ID
   * @param dto 游标、状态和时间筛选条件
   * @return 支付流水视图游标页
   */
  @Override
  public CursorPageVo<PayTransactionVo> pageVo(Long merchantId, PayPageDto dto) {
    return page(merchantId, dto).map(PayTransactionVo::from);
  }

  /**
   * 按状态、时间和主键游标分页查询全平台支付流水实体。
   *
   * @param dto 游标、状态和时间筛选条件
   * @return 平台支付流水实体游标页
   */
  public CursorPageVo<PayTransactionPo> platformPage(PayPageDto dto) {
    Long lastId = CursorUtils.decode(dto.getCursor());
    int limit = dto.getLimit();
    List<PayTransactionPo> records =
        payManager.list(
            Wrappers.lambdaQuery(PayTransactionPo.class)
                .lt(lastId != null, PayTransactionPo::getId, lastId)
                .eq(dto.getStatus() != null, PayTransactionPo::getStatus, dto.getStatus())
                .ge(dto.getStartTime() != null, PayTransactionPo::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, PayTransactionPo::getCreateTime, dto.getEndTime())
                .orderByDesc(PayTransactionPo::getId)
                .last("LIMIT " + (limit + 1)));
    return CursorPageVo.of(records, limit, PayTransactionPo::getId);
  }

  /**
   * 将全平台支付流水游标页转换为对外视图。
   *
   * @param dto 游标、状态和时间筛选条件
   * @return 平台支付流水视图游标页
   */
  @Override
  public CursorPageVo<PayTransactionVo> platformPageVo(PayPageDto dto) {
    return platformPage(dto).map(PayTransactionVo::from);
  }

  /**
   * 统计商家或全平台支付成功金额、退款金额和交易笔数。
   *
   * @param merchantId 商家 ID，为空时统计全平台
   * @param dto 统计时间范围
   * @return 支付统计视图；Mapper 无结果时返回 null
   */
  @Override
  public PayStatisticsVo statistics(Long merchantId, PayPageDto dto) {
    PayStatisticsDto result =
        payManager.statistics(merchantId, dto.getStartTime(), dto.getEndTime());
    if (result == null) {
      return null;
    }
    return new PayStatisticsVo(
        result.getTotalAmount(), result.getRefundAmount(), result.getTransactionCount());
  }

  /**
   * 将指定支付单直接置为冻结状态并持久化。
   * 该操作用于人工异常处置，不校验当前状态。
   *
   * @param paymentNo 支付单号
   */
  public void freeze(String paymentNo) {
    PayTransactionPo po =
        payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPo.class)
                .eq(PayTransactionPo::getPaymentNo, paymentNo));
    if (po == null) {
      throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
    }
    po.setStatus(Integer.parseInt(PayStatusEnum.FROZEN.getCode()));
    payManager.updateById(po);
    log.info("支付单已冻结，paymentNo={}", paymentNo);
  }

  /**
   * 按状态和时间筛选支付流水供 CSV 导出。
   * 结果按创建时间倒序，并限制为公共导出组件的最大行数。
   *
   * @param dto 状态和时间筛选条件
   * @return 待导出的支付流水实体列表
   */
  public List<PayTransactionPo> exportTransactions(PayPageDto dto) {
    return payManager.list(
        Wrappers.lambdaQuery(PayTransactionPo.class)
            .eq(dto.getStatus() != null, PayTransactionPo::getStatus, dto.getStatus())
            .ge(dto.getStartTime() != null, PayTransactionPo::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, PayTransactionPo::getCreateTime, dto.getEndTime())
            .orderByDesc(PayTransactionPo::getCreateTime)
            .last("LIMIT " + CsvExporter.maxExportRows()));
  }

  /**
   * 将按条件查询到的支付流水实体转换为导出视图。
   *
   * @param dto 状态和时间筛选条件
   * @return 支付流水导出视图列表
   */
  @Override
  public List<PayTransactionVo> exportTransactionVos(PayPageDto dto) {
    return exportTransactions(dto).stream().map(PayTransactionVo::from).toList();
  }

  /**
   * 将可选起止日期解析为时间边界后导出支付流水视图。
   * 空或非法日期会忽略对应边界，结束日期包含当天。
   *
   * @param startDate 起始日期字符串，可为空
   * @param endDate 结束日期字符串，可为空
   * @return 支付流水导出视图列表
   */
  @Override
  public List<PayTransactionVo> exportTransactionVos(String startDate, String endDate) {
    PayPageDto dto = new PayPageDto();
    dto.setStartTime(parseStatStart(startDate));
    dto.setEndTime(parseStatEnd(endDate));
    return exportTransactionVos(dto);
  }

  private LocalDateTime parseStatStart(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(date).atStartOfDay();
    } catch (Exception e) {
      log.warn("支付导出中的 startDate 非法，忽略边界。date={}", date);
      return null;
    }
  }

  private LocalDateTime parseStatEnd(String date) {
    if (date == null || date.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(date).atTime(LocalTime.MAX);
    } catch (Exception e) {
      log.warn("支付导出中的 endDate 非法，忽略边界。date={}", date);
      return null;
    }
  }

  /**
   * 生成指定日期区间的平台支付对账单。
   * 起始日期取当天零点，结束日期取次日零点，形成左闭右开查询区间。
   *
   * @param dto 起止日期
   * @return 支付、退款和冻结汇总；Mapper 无结果时返回 null
   */
  @Override
  public PayStatementVo statement(PayStatementDto dto) {
    LocalDateTime startTime = dto.getStartDate().atStartOfDay();
    LocalDateTime endTime = dto.getEndDate().plusDays(1).atStartOfDay();
    PayStatementResultDto result = payManager.statement(startTime, endTime);
    if (result == null) {
      return null;
    }
    PayStatementVo vo = new PayStatementVo();
    vo.setTotalCount(result.getTotalCount());
    vo.setTotalAmount(result.getTotalAmount());
    vo.setPaidCount(result.getPaidCount());
    vo.setPaidAmount(result.getPaidAmount());
    vo.setRefundedCount(result.getRefundedCount());
    vo.setRefundedAmount(result.getRefundedAmount());
    vo.setFrozenCount(result.getFrozenCount());
    vo.setFrozenAmount(result.getFrozenAmount());
    return vo;
  }

  /**
   * 补偿近期已支付但订单可能尚未推进的流水。
   * 仅扫描最近二十四小时、回调冷却三十秒后的成功流水，按 ID 游标最多处理十批，
   * 每批一百条；单笔订单推进失败不阻断后续记录。
   *
   * @return 本次成功推进订单的支付流水数量
   */
  @Override
  public int scanPaidButOrderPending() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime windowStart = now.minusHours(SCAN_WINDOW_HOURS);
    LocalDateTime threshold = now.minusSeconds(PAY_CALLBACK_RPC_BUFFER_SECONDS);
    int successCode = Integer.parseInt(PayStatusEnum.SUCCESS.getCode());
    Long lastId = null;
    int count = 0;
    for (int batch = 0; batch < SCAN_MAX_BATCHES; batch++) {
      List<PayTransactionPo> paid =
          payManager.list(
              Wrappers.lambdaQuery(PayTransactionPo.class)
                  .eq(PayTransactionPo::getStatus, successCode)
                  .ge(PayTransactionPo::getPaidAt, windowStart)
                  .lt(PayTransactionPo::getPaidAt, threshold)
                  .gt(lastId != null, PayTransactionPo::getId, lastId)
                  .orderByAsc(PayTransactionPo::getId)
                  .last("LIMIT " + SCAN_BATCH_SIZE));
      if (paid.isEmpty()) {
        break;
      }
      for (PayTransactionPo po : paid) {
        try {
          Result<Void> result = advanceOrder(po.getOrderNo());
          if (result != null && CommonCode.SUCCESS.equals(result.getCode())) {
            count++;
          }
        } catch (Exception e) {
          log.warn(
              "scanPaidButOrderPending 推进失败，paymentNo={}，orderNo={}，err={}",
              po.getPaymentNo(),
              po.getOrderNo(),
              e.getMessage());
        }
      }
      lastId = paid.get(paid.size() - 1).getId();
      if (paid.size() < SCAN_BATCH_SIZE) {
        break;
      }
    }
    if (count > 0) {
      log.info("scanPaidButOrderPending 已补偿 {} 笔支付", count);
    }
    return count;
  }

  /**
   * 批量抢占并恢复已到重试时间的待退款流水。
   * 单批最多一百条；抢占失败跳过，业务异常保留已有重试计划，其他异常重新排期。
   *
   * @return 本次成功完成的退款数量
   */
  @Override
  public int scanPendingRefunds() {
    LocalDateTime now = LocalDateTime.now();
    List<PayRefundPo> refunds = payRefundManager.listPendingRefunds(now, SCAN_BATCH_SIZE);
    int count = 0;
    for (PayRefundPo refund : refunds) {
      LocalDateTime leaseUntil = now.plusSeconds(REFUND_PROCESSING_LEASE_SECONDS);
      if (payRefundManager.claimPendingRefund(refund.getRefundNo(), now, leaseUntil) == 0) {
        continue;
      }
      try {
        RefundVo result = recoverPendingRefund(refund);
        if (result != null
            && result.getStatus() == Integer.parseInt(RefundStatusEnum.SUCCESS.getCode())) {
          count++;
        }
      } catch (BizException e) {
        log.warn("scanPendingRefunds 已安排重试，refundNo={}，code={}", refund.getRefundNo(), e.getCode());
      } catch (Exception e) {
        scheduleRefundRetry(refund);
        log.error("scanPendingRefunds 恢复失败，refundNo={}", refund.getRefundNo(), e);
      }
    }
    if (count > 0) {
      log.info("scanPendingRefunds 已补偿 {} 笔退款", count);
    }
    return count;
  }

  /**
   * 补发近期成功但未记录通知时间的退款给订单服务。
   * 单次最多扫描一百条；支付单缺失或订单号非数字时跳过，通知成功才计入结果。
   *
   * @return 本次成功通知订单服务的退款数量
   */
  @Override
  public int scanUnnotifiedRefunds() {
    LocalDateTime windowStart = LocalDateTime.now().minusHours(SCAN_WINDOW_HOURS);
    List<PayRefundPo> refunds =
        payRefundManager.list(
            Wrappers.<PayRefundPo>lambdaQuery()
                .eq(PayRefundPo::getStatus, Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()))
                .isNull(PayRefundPo::getNotifiedAt)
                .ge(PayRefundPo::getCreateTime, windowStart)
                .orderByAsc(PayRefundPo::getId)
                .last("LIMIT " + SCAN_BATCH_SIZE));
    int count = 0;
    for (PayRefundPo refund : refunds) {
      PayTransactionPo payTx =
          payManager.getOne(
              Wrappers.<PayTransactionPo>lambdaQuery()
                  .eq(PayTransactionPo::getPaymentNo, refund.getPaymentNo())
                  .last("LIMIT 1"));
      if (payTx == null) {
        log.warn("scanUnnotifiedRefunds 未找到支付单，refundNo={}", refund.getRefundNo());
        continue;
      }
      Long orderId;
      try {
        orderId = Long.valueOf(payTx.getOrderNo());
      } catch (NumberFormatException e) {
        continue;
      }
      BigDecimal totalRefunded = payRefundManager.sumCommittedAmount(refund.getPaymentNo());
      boolean fullyRefunded =
          payTx.getAmount() != null && totalRefunded.compareTo(payTx.getAmount()) >= 0;
      notifyOrderRefund(orderId, payTx, refund, totalRefunded, fullyRefunded);
      if (refund.getNotifiedAt() != null) {
        count++;
      }
    }
    return count;
  }
}
