package com.yirancrazy.minimall.pay.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDTO;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.gateway.PayRemoteGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class PayServiceImpl implements PayService {

    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAY_CALLBACK_RPC_BUFFER_SECONDS = 30;
    private static final int ORDER_PENDING_CODE = 1;
    // 金额统一按“分”存储：两位小数，任何更高精度尾数一律拒绝而非静默进位
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
     * 构造 PayServiceImpl 实例。
     * @param payManager payManager 参数
     * @param payRefundManager payRefundManager 参数
     * @param payGateway payGateway 参数
     * @param payRemoteGateway payRemoteGateway 参数
     * @param transactionManager transactionManager 参数
     */
    public PayServiceImpl(PayManager payManager, PayRefundManager payRefundManager, PayGateway payGateway,
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
     * 创建业务数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param merchantId merchantId 参数
     * @param amount amount 参数
     * @param channel channel 参数
     * @return 业务数值
     */
    @Override
    public Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel) {
        int alipayCode = Integer.parseInt(PayChannelEnum.ALIPAY.getCode());  // 获取支付宝渠道编码（当前仅支持支付宝）
        int channelCode = channel == null ? alipayCode : channel;            // 渠道为空时默认使用支付宝

        // 仅允许支付宝渠道，其他渠道直接抛出不支持异常
        if (channelCode != alipayCode) {
            throw new BizException(PayCodeEnum.PAY_CHANNEL_UNSUPPORTED);
        }

        // 订单快照是支付归属、金额和状态校验的唯一可信来源，客户端 merchantId 不参与决策
        OrderPaySnapshotDTO snapshot = loadPaySnapshot(orderNo);
        validatePaymentRequest(snapshot, userId, amount);
        amount = normalizeAmount(amount);
        merchantId = snapshot.getMerchantId() == null ? 0L : snapshot.getMerchantId();

        // 根据订单号查询已有支付交易记录，用于幂等判断
        PayTransactionPO existing = latestByOrderNo(orderNo);

        // 幂等复用：成功单始终复用；待支付单仅未过期时复用，过期 PENDING 视为失效走新建，
        // 避免残留待支付单被后续请求静默复用导致支付流程无响应也无日志
        if (existing != null) {
            int status = existing.getStatus();
            boolean pendingAlive = status == Integer.parseInt(PayStatusEnum.PENDING.getCode())
                && existing.getExpireAt() != null && existing.getExpireAt().isAfter(LocalDateTime.now());

            // 成功单或未过期的待支付单可复用
            if (status == Integer.parseInt(PayStatusEnum.SUCCESS.getCode()) || pendingAlive) {
                log.info("支付单已复用，paymentNo={}，orderNo={}，status={}",
                    existing.getPaymentNo(), orderNo, status);
                return existing.getId();
            }

            // 过期待支付单视作无效，关闭并释放订单号
            if (status == Integer.parseInt(PayStatusEnum.PENDING.getCode())) {
                log.warn("支付单已过期但仍待支付，创建新支付单，paymentNo={}，orderNo={}，expireAt={}",
                    existing.getPaymentNo(), orderNo, existing.getExpireAt());
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
        PayTransactionPO po = new PayTransactionPO();
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
        }
        catch (DuplicateKeyException e) {
            // 活跃订单唯一键兜底并发创建：返回已存在的活跃支付单，避免同订单多张可支付流水
            PayTransactionPO active = latestByOrderNo(orderNo);
            if (active != null && active.getStatus() != null
                && active.getStatus() != Integer.parseInt(PayStatusEnum.FAILED.getCode())
                && active.getStatus() != Integer.parseInt(PayStatusEnum.CLOSED.getCode())) {
                log.warn("重复创建有效支付单，复用已有支付单，orderNo={}，paymentNo={}",
                    orderNo, active.getPaymentNo());
                return active.getId();
            }
            throw new BizException(PayCodeEnum.PAY_CREATE_CONFLICT);
        }

        // 收银台表单由 getPaymentParams 重新生成，此处不做同步外部调用，避免支付宝不可达时阻塞下单请求
        log.info("支付单已创建，paymentNo={}，orderNo={}，channel={}", paymentNo, orderNo, channelCode);
        return po.getId();
    }


    /**
     * 处理业务事件。
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean handleAlipayCallback(AlipayCallbackDTO dto) {
        Map<String, String> params = dto.getParams();
        try {
            String tradeNo = payGateway.verifyCallback(params);
            String paymentNo = dto.getOutTradeNo();
            String tradeStatus = dto.getTradeStatus();
            boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

            PayCallbackDTO callback = new PayCallbackDTO(
                paymentNo, tradeNo, success, params.toString(), parseTotalAmount(paymentNo, params));
            handleCallback(callback);
            log.info("支付宝回调已处理，paymentNo={}，tradeNo={}，success={}", paymentNo, tradeNo, success);
            return true;
        }
        catch (BizException e) {
            log.error("回调处理失败: {}", e.getMessage());
            return false;
        }
        catch (Exception e) {
            log.error("回调验签失败", e);
            return false;
        }
    }


    /**
     * 处理业务事件。
     * @param dto dto 参数
     */
    public void handleCallback(PayCallbackDTO dto) {
        // 根据支付流水号查询支付单
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, dto.getPaymentNo()));
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
            log.error("回调金额不匹配，支付单已冻结，paymentNo={}，callbackAmount={}，dbAmount={}",
                dto.getPaymentNo(), dto.getTotalAmount(), po.getAmount());
            return;
        }

        if (dto.isSuccess()) {
            applyCallbackSuccess(po, dto);
        }
        else {
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
        }
        catch (NumberFormatException e) {
            log.warn("回调 total_amount 非法，paymentNo={}，totalAmount={}", paymentNo, rawAmount);
            return null;
        }
    }


    private void applyCallbackSuccess(PayTransactionPO po, PayCallbackDTO dto) {
        int status = po.getStatus();
        if (status == Integer.parseInt(PayStatusEnum.REFUNDING.getCode())
            || status == Integer.parseInt(PayStatusEnum.REFUNDED.getCode())
            || status == Integer.parseInt(PayStatusEnum.FROZEN.getCode())) {
            log.warn("成功回调已忽略，支付单不处于可支付状态，paymentNo={}，status={}",
                dto.getPaymentNo(), status);
            return;
        }

        // 推进订单
        Result<Void> advanceResult = advanceOrder(po.getOrderNo());
        if (advanceResult == null || !CommonCode.SUCCESS.equals(advanceResult.getCode())) {
            throw new BizException(PayCodeEnum.PAY_ORDER_ADVANCE_FAILED);
        }

        po.setTradeNo(dto.getTradeNo());
        po.setStatus(Integer.parseInt(PayStatusEnum.SUCCESS.getCode()));
        po.setChannelResponse(dto.getChannelResponse());
        po.setPaidAt(LocalDateTime.now());
        payManager.updateById(po);
    }


    private Result<Void> advanceOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Result.fail(PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getCode(),
                PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getMessage());
        }
        try {
            return orderNo.matches("\\d+")
                ? payRemoteGateway.pay(Long.valueOf(orderNo))
                : payRemoteGateway.payByOrderNo(orderNo);
        }
        catch (Exception e) {
            log.warn("推进订单失败，orderNo={}", orderNo, e);
            return Result.fail(PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getCode(),
                PayCodeEnum.PAY_ORDER_ADVANCE_FAILED.getMessage());
        }
    }


    private void applyCallbackFailure(PayTransactionPO po, PayCallbackDTO dto) {
        int status = po.getStatus();
        if (status != Integer.parseInt(PayStatusEnum.PENDING.getCode())) {
            log.warn("失败回调已忽略，支付单不处于待支付状态，paymentNo={}，status={}",
                dto.getPaymentNo(), status);
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
        }
        catch (ArithmeticException e) {
            throw new BizException(PayCodeEnum.PAY_AMOUNT_SCALE_INVALID);
        }
    }


    private static boolean matchesAmount(BigDecimal callbackAmount, BigDecimal storedAmount) {
        try {
            return callbackAmount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY)
                .compareTo(storedAmount) == 0;
        }
        catch (ArithmeticException e) {
            return false;
        }
    }


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public RefundVO createRefund(RefundCreateDTO dto) {
        PayRefundPO existingRefund = findByRefundRequestNo(dto.getRefundRequestNo());
        if (existingRefund != null && isFinalRefundStatus(existingRefund.getStatus())) {
            log.info("退款请求已复用，requestNo={}，refundNo={}，status={}",
                dto.getRefundRequestNo(), existingRefund.getRefundNo(), existingRefund.getStatus());
            return toRefundVO(existingRefund);
        }

        PayTransactionPO payTx = payManager.getById(dto.getPayId());
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
            existingRefund = reserveRefund(
                payTx, refundAmount, dto.getReason(), refundNo, refundRequestNo);
        }
        catch (DuplicateKeyException e) {
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


    private PayRefundPO reserveRefund(PayTransactionPO payTx, BigDecimal refundAmount, String reason,
                                      String refundNo, String refundRequestNo) {
        return transactionTemplate.execute(status -> {
            BigDecimal committedAmount = payRefundManager.sumCommittedAmount(payTx.getPaymentNo());
            if (committedAmount.add(refundAmount).compareTo(payTx.getAmount()) > 0) {
                throw new BizException(PayCodeEnum.REFUND_EXCEED);
            }

            PayRefundPO refund = new PayRefundPO();
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
            if (payManager.updateStatusIf(
                payTx.getPaymentNo(), successCode, refundingCode) == 0) {
                throw new BizException(PayCodeEnum.REFUND_CONFLICT);
            }
            return refund;
        });
    }


    private RefundVO submitRefund(PayRefundPO refund) {
        try {
            String refundTradeNo = payGateway.refund(
                refund.getPaymentNo(), refund.getRefundNo(), refund.getAmount(), refund.getReason());
            return completeRefund(refund, refundTradeNo);
        }
        catch (Exception e) {
            scheduleRefundRetry(refund);
            log.error("退款网关调用失败，refundNo={}，paymentNo={}",
                refund.getRefundNo(), refund.getPaymentNo(), e);
            throw new BizException(PayCodeEnum.REFUND_FAILED);
        }
    }


    private RefundVO recoverPendingRefund(PayRefundPO refund) {
        PayGateway.RefundQueryStatus queryStatus;
        try {
            queryStatus = payGateway.queryRefund(refund.getPaymentNo(), refund.getRefundNo());
        }
        catch (Exception e) {
            scheduleRefundRetry(refund);
            log.error("查询退款失败，refundNo={}，paymentNo={}",
                refund.getRefundNo(), refund.getPaymentNo(), e);
            throw new BizException(PayCodeEnum.REFUND_FAILED);
        }
        if (queryStatus == PayGateway.RefundQueryStatus.SUCCESS) {
            try {
                return completeRefund(refund, refund.getRefundTradeNo());
            }
            catch (Exception e) {
                scheduleRefundRetry(refund);
                throw new BizException(PayCodeEnum.REFUND_FAILED);
            }
        }
        if (queryStatus == PayGateway.RefundQueryStatus.NOT_FOUND) {
            return submitRefund(refund);
        }
        scheduleRefundRetry(refund);
        log.warn("退款查询仍待处理，refundNo={}，paymentNo={}，status={}",
            refund.getRefundNo(), refund.getPaymentNo(), queryStatus);
        throw new BizException(PayCodeEnum.REFUND_FAILED);
    }


    private RefundVO completeRefund(PayRefundPO refund, String refundTradeNo) {
        PayTransactionPO payTx = payManager.getOne(Wrappers.<PayTransactionPO>lambdaQuery()
            .eq(PayTransactionPO::getPaymentNo, refund.getPaymentNo())
            .last("LIMIT 1"));
        BigDecimal totalRefunded = payRefundManager.sumCommittedAmount(refund.getPaymentNo());
        boolean fullyRefunded = false;
        if (payTx != null && payTx.getAmount() != null) {
            fullyRefunded = totalRefunded.compareTo(payTx.getAmount()) >= 0;
            int targetStatus = fullyRefunded
                ? Integer.parseInt(PayStatusEnum.REFUNDED.getCode())
                : Integer.parseInt(PayStatusEnum.SUCCESS.getCode());
            payManager.updateStatusIf(
                refund.getPaymentNo(), Integer.parseInt(PayStatusEnum.REFUNDING.getCode()), targetStatus);
        }

        int updated = payRefundManager.updateStatusIf(
            refund.getRefundNo(), Integer.parseInt(RefundStatusEnum.PENDING.getCode()),
            Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()), refundTradeNo);
        if (updated == 0) {
            PayRefundPO current = findByRefundRequestNo(refund.getRefundRequestNo());
            if (current != null
                && current.getStatus() == Integer.parseInt(RefundStatusEnum.SUCCESS.getCode())) {
                return toRefundVO(current);
            }
            throw new BizException(PayCodeEnum.REFUND_CONFLICT);
        }

        refund.setStatus(Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()));
        refund.setRefundTradeNo(refundTradeNo);
        Long orderId = resolveOrderId(payTx);
        if (orderId != null) {
            notifyOrderRefund(orderId, payTx, refund, totalRefunded, fullyRefunded);
        }
        log.info("退款成功，refundNo={}，paymentNo={}",
            refund.getRefundNo(), refund.getPaymentNo());
        return toRefundVO(refund);
    }


    private void scheduleRefundRetry(PayRefundPO refund) {
        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(REFUND_RETRY_DELAY_SECONDS);
        refund.setNextRetryAt(nextRetryAt);
        int retryCount = refund.getRetryCount() == null ? 0 : refund.getRetryCount();
        refund.setRetryCount(retryCount + 1);
        try {
            payRefundManager.scheduleRefundRetry(refund.getRefundNo(), nextRetryAt);
        }
        catch (Exception e) {
            log.error("定时退款重试失败，refundNo={}", refund.getRefundNo(), e);
        }
    }

    private static Long resolveOrderId(PayTransactionPO payTx) {
        if (payTx == null || payTx.getOrderNo() == null) {
            return null;
        }
        try {
            return Long.valueOf(payTx.getOrderNo());
        }
        catch (NumberFormatException e) {
            log.warn("orderNo 非数字，跳过退款后订单推进，paymentNo={}，orderNo={}",
                payTx.getPaymentNo(), payTx.getOrderNo());
            return null;
        }
    }

    private static boolean isFinalRefundStatus(Integer status) {
        return status != null && status != Integer.parseInt(RefundStatusEnum.PENDING.getCode());
    }

    private static void validateExistingRefund(PayRefundPO refund, PayTransactionPO payTx,
                                               BigDecimal refundAmount) {
        if (!payTx.getPaymentNo().equals(refund.getPaymentNo())
            || refund.getAmount().compareTo(refundAmount) != 0) {
            throw new BizException(PayCodeEnum.REFUND_CONFLICT);
        }
    }

    private PayRefundPO findByRefundRequestNo(String refundRequestNo) {
        if (refundRequestNo == null || refundRequestNo.isBlank()) {
            return null;
        }
        return payRefundManager.findByRefundRequestNo(refundRequestNo);
    }

    private static RefundVO toRefundVO(PayRefundPO refund) {
        return new RefundVO(
            refund.getRefundNo(), refund.getPaymentNo(), refund.getAmount(), refund.getStatus());
    }


    private void notifyOrderRefund(Long orderId, PayTransactionPO payTx, PayRefundPO refund,
                                   BigDecimal totalRefunded, boolean fullyRefunded) {
        try {
            RefundCallbackDTO dto = new RefundCallbackDTO(
                orderId, payTx.getId(), refund.getAmount(), totalRefunded,
                fullyRefunded, refund.getRefundRequestNo());
            Result<Void> result = payRemoteGateway.refundCallback(dto);
            if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
                throw new IllegalStateException("订单退款回调被拒绝："
                    + (result == null ? "null" : result.getCode()));
            }
            refund.setNotifiedAt(LocalDateTime.now());
            payRefundManager.updateById(refund);
        }
        catch (Exception e) {
            log.error("订单退款回调失败，将重试，orderId={}，refundNo={}",
                orderId, refund.getRefundNo(), e);
        }
    }

    private String generateRefundNo() {
        return "REFUND" + requireGlobalId("REFUND");
    }


    private Long requireGlobalId(String bizTag) {
        try {
            Result<Long> result = payRemoteGateway.nextId(bizTag);
            if (result != null && CommonCode.SUCCESS.equals(result.getCode()) && result.getData() != null) {
                return result.getData();
            }
            log.error("id-service 返回失败，bizTag={}，result={}", bizTag, result);
        }
        catch (Exception e) {
            log.error("id-service nextId 失败，bizTag={}", bizTag, e);
        }
        throw new BizException(PayCodeEnum.PAY_ID_GENERATE_FAIL);
    }


    /**
     * 获取相关数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    public PayTransactionPO getByOrderNo(String orderNo, Long userId) {
        PayTransactionPO po = latestByOrderNo(orderNo);
        if (po == null || !ownedBy(po, userId)) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        return po;
    }


    /**
     * 获取相关数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public PayTransactionVO getStatusVO(String orderNo, Long userId) {
        return PayTransactionVO.from(getByOrderNo(orderNo, userId));
    }


    private static boolean ownedBy(PayTransactionPO po, Long userId) {
        return po.getUserId() != null && po.getUserId().equals(userId);
    }

    private PayTransactionPO latestByOrderNo(String orderNo) {
        return payManager.getOne(Wrappers.lambdaQuery(PayTransactionPO.class)
            .eq(PayTransactionPO::getOrderNo, orderNo)
            .orderByDesc(PayTransactionPO::getId)
            .last("LIMIT 1"));
    }


    private OrderPaySnapshotDTO loadPaySnapshot(String orderNo) {
        Result<OrderPaySnapshotDTO> result;

        // 加载订单支付快照
        try {
            result = payRemoteGateway.paySnapshot(orderNo);
        }
        catch (Exception e) {
            log.warn("加载订单支付快照失败，orderNo={}", orderNo, e);
            throw new BizException(CommonCode.SYS_ERROR, "订单服务不可用");
        }
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode()) || result.getData() == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        return result.getData();
    }


    private void validatePaymentRequest(OrderPaySnapshotDTO snapshot, Long userId, BigDecimal amount) {
        if (snapshot.getUserId() == null || !snapshot.getUserId().equals(userId)) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        if (snapshot.getPayAmount() == null || amount == null
            || snapshot.getPayAmount().compareTo(amount) != 0) {
            throw new BizException(PayCodeEnum.PAY_AMOUNT_MISMATCH);
        }
    }


    /**
     * 获取相关数据。
     * @param paymentNo paymentNo 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public PaymentParamsVO getPaymentParams(String paymentNo, Long userId) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, paymentNo));
        if (po == null || !ownedBy(po, userId)) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        String payForm = payGateway.createPagePayment(
            po.getPaymentNo(), po.getAmount(), "Order " + po.getOrderNo(),
            po.getExpireAt().format(EXPIRE_FORMATTER));
        return new PaymentParamsVO(
            po.getPaymentNo(), po.getOrderNo(), po.getAmount(), po.getCurrency(),
            po.getChannel(), "Order " + po.getOrderNo(), po.getExpireAt(), payForm);
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<PayTransactionPO> page(Long merchantId, PayPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<PayTransactionPO> records = payManager.list(Wrappers.lambdaQuery(PayTransactionPO.class)
            .lt(lastId != null, PayTransactionPO::getId, lastId)
            .eq(PayTransactionPO::getMerchantId, merchantId)
            .eq(dto.getStatus() != null, PayTransactionPO::getStatus, dto.getStatus())
            .ge(dto.getStartTime() != null, PayTransactionPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, PayTransactionPO::getCreateTime, dto.getEndTime())
            .orderByDesc(PayTransactionPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, PayTransactionPO::getId);
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<PayTransactionVO> pageVO(Long merchantId, PayPageDTO dto) {
        return page(merchantId, dto).map(PayTransactionVO::from);
    }


    /**
     * 执行 platformPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<PayTransactionPO> platformPage(PayPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<PayTransactionPO> records = payManager.list(Wrappers.lambdaQuery(PayTransactionPO.class)
            .lt(lastId != null, PayTransactionPO::getId, lastId)
            .eq(dto.getStatus() != null, PayTransactionPO::getStatus, dto.getStatus())
            .ge(dto.getStartTime() != null, PayTransactionPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, PayTransactionPO::getCreateTime, dto.getEndTime())
            .orderByDesc(PayTransactionPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, PayTransactionPO::getId);
    }


    /**
     * 执行 platformPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<PayTransactionVO> platformPageVO(PayPageDTO dto) {
        return platformPage(dto).map(PayTransactionVO::from);
    }


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public PayStatisticsVO statistics(Long merchantId, PayPageDTO dto) {
        PayStatisticsDTO result = payManager.statistics(merchantId, dto.getStartTime(), dto.getEndTime());
        if (result == null) {
            return null;
        }
        return new PayStatisticsVO(
            result.getTotalAmount(), result.getRefundAmount(), result.getTransactionCount());
    }


    /**
     * 执行 freeze 方法。
     * @param paymentNo paymentNo 参数
     */
    public void freeze(String paymentNo) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, paymentNo));
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        po.setStatus(Integer.parseInt(PayStatusEnum.FROZEN.getCode()));
        payManager.updateById(po);
        log.info("支付单已冻结，paymentNo={}", paymentNo);
    }


    /**
     * 执行 exportTransactions 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    public List<PayTransactionPO> exportTransactions(PayPageDTO dto) {
        return payManager.list(Wrappers.lambdaQuery(PayTransactionPO.class)
            .eq(dto.getStatus() != null, PayTransactionPO::getStatus, dto.getStatus())
            .ge(dto.getStartTime() != null, PayTransactionPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, PayTransactionPO::getCreateTime, dto.getEndTime())
            .orderByDesc(PayTransactionPO::getCreateTime)
            .last("LIMIT " + CsvExporter.maxExportRows()));
    }


    /**
     * 执行 exportTransactionVOs 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    @Override
    public List<PayTransactionVO> exportTransactionVOs(PayPageDTO dto) {
        return exportTransactions(dto).stream().map(PayTransactionVO::from).toList();
    }

    /**
     * 执行 exportTransactionVOs 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    @Override
    public List<PayTransactionVO> exportTransactionVOs(String startDate, String endDate) {
        PayPageDTO dto = new PayPageDTO();
        dto.setStartTime(parseStatStart(startDate));
        dto.setEndTime(parseStatEnd(endDate));
        return exportTransactionVOs(dto);
    }


    private LocalDateTime parseStatStart(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay();
        }
        catch (Exception e) {
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
        }
        catch (Exception e) {
            log.warn("支付导出中的 endDate 非法，忽略边界。date={}", date);
            return null;
        }
    }


    /**
     * 执行 statement 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public PayStatementVO statement(PayStatementDTO dto) {
        LocalDateTime startTime = dto.getStartDate().atStartOfDay();
        LocalDateTime endTime = dto.getEndDate().plusDays(1).atStartOfDay();
        PayStatementResultDTO result = payManager.statement(startTime, endTime);
        if (result == null) {
            return null;
        }
        PayStatementVO vo = new PayStatementVO();
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
     * 扫描并处理待办数据。
     * @return 业务数值
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
            List<PayTransactionPO> paid = payManager.list(Wrappers.lambdaQuery(PayTransactionPO.class)
                .eq(PayTransactionPO::getStatus, successCode)
                .ge(PayTransactionPO::getPaidAt, windowStart)
                .lt(PayTransactionPO::getPaidAt, threshold)
                .gt(lastId != null, PayTransactionPO::getId, lastId)
                .orderByAsc(PayTransactionPO::getId)
                .last("LIMIT " + SCAN_BATCH_SIZE));
            if (paid.isEmpty()) {
                break;
            }
            for (PayTransactionPO po : paid) {
                try {
                    Result<Void> result = advanceOrder(po.getOrderNo());
                    if (result != null && CommonCode.SUCCESS.equals(result.getCode())) {
                        count++;
                    }
                }
                catch (Exception e) {
                    log.warn("scanPaidButOrderPending 推进失败，paymentNo={}，orderNo={}，err={}",
                        po.getPaymentNo(), po.getOrderNo(), e.getMessage());
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
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    @Override
    public int scanPendingRefunds() {
        LocalDateTime now = LocalDateTime.now();
        List<PayRefundPO> refunds = payRefundManager.listPendingRefunds(now, SCAN_BATCH_SIZE);
        int count = 0;
        for (PayRefundPO refund : refunds) {
            LocalDateTime leaseUntil = now.plusSeconds(REFUND_PROCESSING_LEASE_SECONDS);
            if (payRefundManager.claimPendingRefund(refund.getRefundNo(), now, leaseUntil) == 0) {
                continue;
            }
            try {
                RefundVO result = recoverPendingRefund(refund);
                if (result != null
                    && result.getStatus() == Integer.parseInt(RefundStatusEnum.SUCCESS.getCode())) {
                    count++;
                }
            }
            catch (BizException e) {
                log.warn("scanPendingRefunds 已安排重试，refundNo={}，code={}",
                    refund.getRefundNo(), e.getCode());
            }
            catch (Exception e) {
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
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    @Override
    public int scanUnnotifiedRefunds() {
        LocalDateTime windowStart = LocalDateTime.now().minusHours(SCAN_WINDOW_HOURS);
        List<PayRefundPO> refunds = payRefundManager.list(
            Wrappers.<PayRefundPO>lambdaQuery()
                .eq(PayRefundPO::getStatus, Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()))
                .isNull(PayRefundPO::getNotifiedAt)
                .ge(PayRefundPO::getCreateTime, windowStart)
                .orderByAsc(PayRefundPO::getId)
                .last("LIMIT " + SCAN_BATCH_SIZE));
        int count = 0;
        for (PayRefundPO refund : refunds) {
            PayTransactionPO payTx = payManager.getOne(Wrappers.<PayTransactionPO>lambdaQuery()
                .eq(PayTransactionPO::getPaymentNo, refund.getPaymentNo())
                .last("LIMIT 1"));
            if (payTx == null) {
                log.warn("scanUnnotifiedRefunds 未找到支付单，refundNo={}", refund.getRefundNo());
                continue;
            }
            Long orderId;
            try {
                orderId = Long.valueOf(payTx.getOrderNo());
            }
            catch (NumberFormatException e) {
                continue;
            }
            BigDecimal totalRefunded = payRefundManager.sumCommittedAmount(refund.getPaymentNo());
            boolean fullyRefunded = payTx.getAmount() != null
                && totalRefunded.compareTo(payTx.getAmount()) >= 0;
            notifyOrderRefund(orderId, payTx, refund, totalRefunded, fullyRefunded);
            if (refund.getNotifiedAt() != null) {
                count++;
            }
        }
        return count;
    }
}
