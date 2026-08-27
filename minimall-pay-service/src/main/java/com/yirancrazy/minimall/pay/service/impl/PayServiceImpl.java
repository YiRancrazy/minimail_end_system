package com.yirancrazy.minimall.pay.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务实现，处理核心业务逻辑。
 * @Version: 1.1
 * @DateTime: 2026/08/02
 **/
@Slf4j
@Service
public class PayServiceImpl implements PayService {

    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAY_CALLBACK_RPC_BUFFER_SECONDS = 30;
    private static final int ORDER_PENDING_CODE = 1;
    // 对账补偿扫描分批参数：单批 100 条、单次最多 10 批，仅扫描近 24h 成功流水，限制单次调度扫描量与耗时
    private static final int SCAN_BATCH_SIZE = 100;
    private static final int SCAN_MAX_BATCHES = 10;
    private static final long SCAN_WINDOW_HOURS = 24L;

    private final PayManager payManager;
    private final PayGateway payGateway;
    private final PayRefundMapper payRefundMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final OrderFeignClient orderFeignClient;

    public PayServiceImpl(PayManager payManager, PayGateway payGateway,
                          PayRefundMapper payRefundMapper, PayTransactionMapper payTransactionMapper,
                          OrderFeignClient orderFeignClient) {
        this.payManager = payManager;
        this.payGateway = payGateway;
        this.payRefundMapper = payRefundMapper;
        this.payTransactionMapper = payTransactionMapper;
        this.orderFeignClient = orderFeignClient;
    }

    /**
     * 创建支付流水，channel 为空时默认 ALIPAY，不支持渠道抛出 PAY_CHANNEL_UNSUPPORTED。
     * 同一订单已有待支付/已成功流水时直接复用（防重复扣款），上次支付失败/关闭时新建流水以支持重新支付。
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param merchantId 商户ID，null 时归属默认商户 0L
     * @param amount 支付金额
     * @param channel 支付渠道 code，null 默认 ALIPAY
     * @return 支付流水ID
     */
    @Override
    public Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel) {
        int alipayCode = Integer.parseInt(PayChannelEnum.ALIPAY.getCode());  // 获取支付宝渠道编码（当前仅支持支付宝）
        int channelCode = channel == null ? alipayCode : channel;            // 渠道为空时默认使用支付宝

        // 仅允许支付宝渠道，其他渠道直接抛出不支持异常
        if (channelCode != alipayCode) {
            throw new BizException(PayCodeEnum.PAY_CHANNEL_UNSUPPORTED);
        }

        // 商户上下文缺失（C 端支付无 X-Merchant-Id）时按订单归属解析，解析失败归属默认商户 0L
        if (merchantId == null) {
            merchantId = resolveMerchantId(orderNo);
        }
        merchantId = merchantId == null ? 0L : merchantId;

        // 根据订单号查询已有支付交易记录，用于幂等判断
        PayTransactionPO existing = latestByOrderNo(orderNo);

        // 幂等复用：成功单始终复用；待支付单仅未过期时复用，过期 PENDING 视为失效走新建，
        // 避免残留待支付单被后续请求静默复用导致支付流程无响应也无日志
        if (existing != null) {
            int status = existing.getStatus();
            boolean pendingAlive = status == Integer.parseInt(PayStatusEnum.PENDING.getCode())
                && existing.getExpireAt() != null && existing.getExpireAt().isAfter(LocalDateTime.now());
            if (status == Integer.parseInt(PayStatusEnum.SUCCESS.getCode()) || pendingAlive) {
                log.info("payment reused, paymentNo={}, orderNo={}, status={}",
                    existing.getPaymentNo(), orderNo, status);
                return existing.getId();
            }
            if (status == Integer.parseInt(PayStatusEnum.PENDING.getCode())) {
                log.warn("payment expired pending, creating new one, paymentNo={}, orderNo={}, expireAt={}",
                    existing.getPaymentNo(), orderNo, existing.getExpireAt());
            }
        }

        // 生成支付流水号
        String paymentNo = generatePaymentNo();

        // 支付过期时间设置为当前时间15分钟后
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(15);

        // 组装新的支付交易记录
        PayTransactionPO po = new PayTransactionPO();
        po.setPaymentNo(paymentNo);
        po.setOrderNo(orderNo);
        po.setUserId(userId);
        po.setMerchantId(merchantId);
        po.setAmount(amount);
        po.setCurrency("CNY");
        po.setStatus(Integer.parseInt(PayStatusEnum.PENDING.getCode()));
        po.setChannel(channelCode);
        po.setExpireAt(expireAt);
        po.setIdempotencyKey(UUID.randomUUID().toString());

        // 保存支付交易记录到数据库
        payManager.save(po);

        // 收银台表单由 getPaymentParams 重新生成，此处不做同步外部调用，避免支付宝不可达时阻塞下单请求
        log.info("payment created, paymentNo={}, orderNo={}, channel={}", paymentNo, orderNo, channelCode);
        return po.getId();
    }

    /**
     * 处理支付回调（验签已在 Controller 完成）：幂等去重、金额校验、状态防倒灌，
     * 成功后通过 Feign 推进订单状态，触发 OrderPaidDTO 事件广播。
     * @param dto 支付回调DTO
     */
    @Override
    public void handleCallback(PayCallbackDTO dto) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, dto.getPaymentNo()));
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }

        // 幂等：支付单已 SUCCESS 时直接返回，避免重复推进订单与重复回调 order 服务
        if (po.getStatus() == Integer.parseInt(PayStatusEnum.SUCCESS.getCode())) {
            log.info("callback ignored, payment already success, paymentNo={}", dto.getPaymentNo());
            return;
        }

        // 金额校验：回调金额与库内支付单不符时冻结并告警，返回 success 避免支付宝重复投递
        if (dto.getTotalAmount() != null && dto.getTotalAmount().compareTo(po.getAmount()) != 0) {
            po.setStatus(Integer.parseInt(PayStatusEnum.FROZEN.getCode()));
            po.setChannelResponse(dto.getChannelResponse());
            payManager.updateById(po);
            log.error("callback amount mismatch, payment frozen, paymentNo={}, callbackAmount={}, dbAmount={}",
                dto.getPaymentNo(), dto.getTotalAmount(), po.getAmount());
            return;
        }

        if (dto.isSuccess()) {
            applyCallbackSuccess(po, dto);
        }
        else {
            applyCallbackFailure(po, dto);
        }
        log.info("payment callback handled, paymentNo={}, success={}", dto.getPaymentNo(), dto.isSuccess());
    }

    /**
     * 成功回调迁移：非终态（PENDING/FAILED/CLOSED）置 SUCCESS 并推进订单；
     * 退款中/已退款/已冻结等终态不再迁移，防止订单状态与支付单脱节。
     * @param po 支付单
     * @param dto 回调DTO
     */
    private void applyCallbackSuccess(PayTransactionPO po, PayCallbackDTO dto) {
        int status = po.getStatus();
        if (status == Integer.parseInt(PayStatusEnum.REFUNDING.getCode())
            || status == Integer.parseInt(PayStatusEnum.REFUNDED.getCode())
            || status == Integer.parseInt(PayStatusEnum.FROZEN.getCode())) {
            log.warn("callback success ignored, payment not in payable state, paymentNo={}, status={}",
                dto.getPaymentNo(), status);
            return;
        }

        po.setTradeNo(dto.getTradeNo());
        po.setStatus(Integer.parseInt(PayStatusEnum.SUCCESS.getCode()));
        po.setChannelResponse(dto.getChannelResponse());
        po.setPaidAt(LocalDateTime.now());
        payManager.updateById(po);

        // 订单标识可能为订单ID（内部下单路径）或业务单号（C 端直付路径），按数字与否分流推进订单
        if (po.getOrderNo() != null) {
            if (po.getOrderNo().matches("\\d+")) {
                orderFeignClient.pay(Long.valueOf(po.getOrderNo()));
            }
            else {
                orderFeignClient.payByOrderNo(po.getOrderNo());
            }
        }
    }

    /**
     * 失败回调迁移：仅 PENDING 允许置 FAILED，已 SUCCESS 或终态不回退（防状态倒灌），
     * 避免订单已推进后支付单被失败回调覆盖导致状态脱节。
     * @param po 支付单
     * @param dto 回调DTO
     */
    private void applyCallbackFailure(PayTransactionPO po, PayCallbackDTO dto) {
        int status = po.getStatus();
        if (status != Integer.parseInt(PayStatusEnum.PENDING.getCode())) {
            log.warn("callback failure ignored, payment not pending, paymentNo={}, status={}",
                dto.getPaymentNo(), status);
            return;
        }
        po.setStatus(Integer.parseInt(PayStatusEnum.FAILED.getCode()));
        po.setChannelResponse(dto.getChannelResponse());
        payManager.updateById(po);
    }

    private String generatePaymentNo() {
        return PAYMENT_NO_PREFIX + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 创建退款。先原子抢占支付单为 REFUNDING（并发退款影响 0 行直接拒绝），再校验累计退款上限，
     * 成功后通过 Feign 通知 order 服务推进退款状态；部分退款未退满时支付单回到 SUCCESS 允许继续退款。
     * @param dto 退款创建DTO
     * @return 退款VO
     */
    @Override
    public RefundVO createRefund(RefundCreateDTO dto) {
        PayTransactionPO payTx = payManager.getById(dto.getPayId());

        if (payTx == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        if (payTx.getStatus() != Integer.parseInt(PayStatusEnum.SUCCESS.getCode())) {
            throw new BizException(PayCodeEnum.PAY_NOT_SUCCESS);
        }
        if (dto.getAmount().compareTo(payTx.getAmount()) > 0) {
            throw new BizException(PayCodeEnum.REFUND_AMOUNT_EXCEED);
        }

        String refundNo = generateRefundNo();
        String paymentNo = payTx.getPaymentNo();
        int successCode = Integer.parseInt(PayStatusEnum.SUCCESS.getCode());
        int refundingCode = Integer.parseInt(PayStatusEnum.REFUNDING.getCode());

        // 原子抢占：仅当支付单仍为 SUCCESS 时置为 REFUNDING，并发退款第二个请求影响 0 行直接拒绝，杜绝双重退款
        if (payTransactionMapper.updateStatusIf(paymentNo, successCode, refundingCode) == 0) {
            throw new BizException(PayCodeEnum.REFUND_CONFLICT);
        }

        // 累计校验：已占用退款（在途 + 成功）+ 本次金额不得超过支付金额，超限回滚抢占状态
        BigDecimal committedAmount = payRefundMapper.sumCommittedAmount(paymentNo);
        if (dto.getAmount().add(committedAmount).compareTo(payTx.getAmount()) > 0) {
            payTransactionMapper.updateStatusIf(paymentNo, refundingCode, successCode);
            throw new BizException(PayCodeEnum.REFUND_EXCEED);
        }

        PayRefundPO refund = new PayRefundPO();
        refund.setRefundNo(refundNo);
        refund.setPaymentNo(paymentNo);
        refund.setAmount(dto.getAmount());
        refund.setReason(dto.getReason());
        refund.setStatus(Integer.parseInt(RefundStatusEnum.PENDING.getCode()));
        refund.setIdempotencyKey(UUID.randomUUID().toString());
        payRefundMapper.insert(refund);

        // C 端直付订单号可能为非数字业务单号，OrderFeignClient 仅有按订单ID的退款回调，
        // 无法推进时记 WARN 跳过订单推进，避免 NumberFormatException 导致退款中断
        Long orderId = null;
        try {
            orderId = Long.valueOf(payTx.getOrderNo());
        }
        catch (NumberFormatException e) {
            log.warn("refund skip order advance for non-numeric orderNo, paymentNo={}, orderNo={}",
                paymentNo, payTx.getOrderNo());
        }

        try {
            String refundTradeNo = payGateway.refund(paymentNo, refundNo, dto.getAmount(), dto.getReason());
            refund.setStatus(Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()));
            refund.setRefundTradeNo(refundTradeNo);
            refund.setNotifiedAt(LocalDateTime.now());
            payRefundMapper.updateById(refund);

            // 部分退款语义：累计退满支付金额才置 REFUNDED，未退满回到 SUCCESS 允许继续部分退款
            BigDecimal totalRefunded = payRefundMapper.sumCommittedAmount(paymentNo);
            int targetStatus = totalRefunded.compareTo(payTx.getAmount()) >= 0
                ? Integer.parseInt(PayStatusEnum.REFUNDED.getCode()) : successCode;
            payTransactionMapper.updateStatusIf(paymentNo, refundingCode, targetStatus);

            if (orderId != null) {
                orderFeignClient.refundCallback(orderId, true);
            }
            log.info("refund success, refundNo={}, paymentNo={}", refundNo, paymentNo);
        }
        catch (Exception e) {
            refund.setStatus(Integer.parseInt(RefundStatusEnum.FAILED.getCode()));
            payRefundMapper.updateById(refund);
            payTransactionMapper.updateStatusIf(paymentNo, refundingCode, successCode);
            if (orderId != null) {
                orderFeignClient.refundCallback(orderId, false);
            }
            throw new BizException(PayCodeEnum.REFUND_FAILED);
        }

        return new RefundVO(refundNo, paymentNo, dto.getAmount(), refund.getStatus());
    }

    private String generateRefundNo() {
        return "REFUND" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 按订单号查询支付流水（取最近一条，兼容同一订单多次支付流水），不存在时抛出 PAY_NOT_FOUND。
     * @param orderNo 订单号
     * @return 支付流水实体
     */
    @Override
    public PayTransactionPO getByOrderNo(String orderNo) {
        PayTransactionPO po = latestByOrderNo(orderNo);
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        return po;
    }

    /**
     * 查询指定订单号最近一条支付流水，无则返回 null。
     * @param orderNo 订单号
     * @return 支付流水实体或 null
     */
    private PayTransactionPO latestByOrderNo(String orderNo) {
        return payManager.getOne(Wrappers.lambdaQuery(PayTransactionPO.class)
            .eq(PayTransactionPO::getOrderNo, orderNo)
            .orderByDesc(PayTransactionPO::getId)
            .last("LIMIT 1"));
    }

    /**
     * 按订单标识解析订单归属商户，order-service 不可达或订单不存在时返回 null。
     * @param orderNo 订单标识（订单ID或业务单号）
     * @return 商户ID或 null
     */
    private Long resolveMerchantId(String orderNo) {
        try {
            Result<Long> resp = orderFeignClient.merchantId(orderNo);
            return resp == null ? null : resp.getData();
        }
        catch (Exception e) {
            log.warn("resolve order merchant failed, orderNo={}, fallback to default merchant", orderNo);
            return null;
        }
    }

    /**
     * 按支付单号查询支付参数，供前端调起渠道收银台，不存在时抛出 PAY_NOT_FOUND。
     * page.pay 每次调用重新生成跳转表单返回给前端；支付宝网关异常时抛出并由全局异常处理返回错误。
     * @param paymentNo 支付单号
     * @return 支付参数VO
     */
    @Override
    public PaymentParamsVO getPaymentParams(String paymentNo) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, paymentNo));
        if (po == null) {
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
     * 商家资金流水游标分页查询，merchantId 强制绑定，支持按状态与时间范围过滤。
     * @param merchantId 商家ID
     * @param dto 游标分页查询入参
     * @return 支付流水游标分页结果
     */
    @Override
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
     * 平台全平台交易流水游标分页查询，不绑定 merchantId。
     * @param dto 游标分页查询入参
     * @return 支付流水游标分页结果
     */
    @Override
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
     * 资金统计聚合查询，merchantId 为空时统计全平台。
     * @param merchantId 商家ID，null 表示全平台
     * @param dto 分页查询入参（复用时间范围字段）
     * @return 统计VO
     */
    @Override
    public PayStatisticsVO statistics(Long merchantId, PayPageDTO dto) {
        return payTransactionMapper.statistics(merchantId, dto.getStartTime(), dto.getEndTime());
    }

    /**
     * 异常支付冻结，将支付单状态置为 FROZEN，不存在时抛出 PAY_NOT_FOUND。
     * @param paymentNo 支付单号
     */
    @Override
    public void freeze(String paymentNo) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, paymentNo));
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        po.setStatus(Integer.parseInt(PayStatusEnum.FROZEN.getCode()));
        payManager.updateById(po);
        log.info("payment frozen, paymentNo={}", paymentNo);
    }

    /**
     * 导出全平台交易流水对账单，最多 10000 行。
     * @param dto 查询入参
     * @return 支付流水列表
     */
    @Override
    public List<PayTransactionPO> exportTransactions(PayPageDTO dto) {
        return payManager.list(Wrappers.lambdaQuery(PayTransactionPO.class)
            .eq(dto.getStatus() != null, PayTransactionPO::getStatus, dto.getStatus())
            .ge(dto.getStartTime() != null, PayTransactionPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, PayTransactionPO::getCreateTime, dto.getEndTime())
            .orderByDesc(PayTransactionPO::getCreateTime)
            .last("LIMIT " + CsvExporter.maxExportRows()));
    }

    /**
     * 对账单聚合查询，将日期范围转为时间范围后委托给 mapper.statement。
     * @param dto 对账单查询入参（含起止日期）
     * @return 对账单 VO
     */
    @Override
    public PayStatementVO statement(PayStatementDTO dto) {
        LocalDateTime startTime = dto.getStartDate().atStartOfDay();
        LocalDateTime endTime = dto.getEndDate().plusDays(1).atStartOfDay();
        return payTransactionMapper.statement(startTime, endTime);
    }

    /**
     * 扫描支付成功但订单状态仍为 PENDING 的流水（paidAt 落在 [now-24h, now-30s) 时间窗内），主动调 Order RPC 兜底推进。
     * 按 id 升序每批 100 条游标推进（id > lastId），单次调用最多 10 批即返回，防止全表扫描随流水量增长拖垮数据库；
     * 幂等由订单状态探测保证（订单已非 PENDING 则跳过），Feign 失败仅记 WARN 不阻断后续批次。
     * @return 处理的流水数
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
                Long orderId;
                try {
                    orderId = Long.valueOf(po.getOrderNo());
                }
                catch (NumberFormatException e) {
                    log.warn("scanPaidButOrderPending skip non-numeric orderNo, paymentNo={}", po.getPaymentNo());
                    continue;
                }
                try {
                    Integer orderStatus = orderFeignClient.status(orderId).getData();
                    if (orderStatus != null && orderStatus == ORDER_PENDING_CODE) {
                        orderFeignClient.pay(orderId);
                        count++;
                    }
                }
                catch (Exception e) {
                    log.warn("scanPaidButOrderPending probe failed, paymentNo={}, orderId={}, err={}",
                        po.getPaymentNo(), orderId, e.getMessage());
                }
            }
            lastId = paid.get(paid.size() - 1).getId();
            if (paid.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }
        if (count > 0) {
            log.info("scanPaidButOrderPending compensated {} payments", count);
        }
        return count;
    }
}