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
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
import com.yirancrazy.minimall.pay.constant.WithdrawStatusEnum;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;
import com.yirancrazy.minimall.pay.vo.WithdrawVO;


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
    private static final String WITHDRAW_NO_PREFIX = "WD";
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PayManager payManager;
    private final AlipayGateway alipayGateway;
    private final PayRefundMapper payRefundMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final OrderFeignClient orderFeignClient;
    private final MerchantWithdrawManager merchantWithdrawManager;

    public PayServiceImpl(PayManager payManager, AlipayGateway alipayGateway,
                          PayRefundMapper payRefundMapper, PayTransactionMapper payTransactionMapper,
                          OrderFeignClient orderFeignClient,
                          MerchantWithdrawManager merchantWithdrawManager) {
        this.payManager = payManager;
        this.alipayGateway = alipayGateway;
        this.payRefundMapper = payRefundMapper;
        this.payTransactionMapper = payTransactionMapper;
        this.orderFeignClient = orderFeignClient;
        this.merchantWithdrawManager = merchantWithdrawManager;
    }

    /**
     * 创建支付流水，channel 为空时默认 ALIPAY，不支持渠道抛出 PAY_CHANNEL_UNSUPPORTED。
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param merchantId 商户ID
     * @param amount 支付金额
     * @param channel 支付渠道 code，null 默认 ALIPAY
     * @return 支付流水ID
     */
    @Override
    public Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel) {
        int alipayCode = Integer.parseInt(PayChannelEnum.ALIPAY.getCode());
        int channelCode = channel == null ? alipayCode : channel;
        if (channelCode != alipayCode) {
            throw new BizException(PayCodeEnum.PAY_CHANNEL_UNSUPPORTED);
        }
        String paymentNo = generatePaymentNo();
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(15);

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
        payManager.save(po);

        String expireTime = expireAt.format(EXPIRE_FORMATTER);
        alipayGateway.createPayment(paymentNo, amount, "Order " + orderNo, expireTime);
        log.info("payment created, paymentNo={}, orderNo={}, channel={}", paymentNo, orderNo, channelCode);
        return po.getId();
    }

    /**
     * 处理支付回调。成功时通过 Feign 推进订单状态，触发 OrderPaidDTO 事件广播。
     * @param dto 支付回调DTO
     */
    @Override
    public void handleCallback(PayCallbackDTO dto) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, dto.getPaymentNo()));
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }

        po.setTradeNo(dto.getTradeNo());
        po.setStatus(Integer.parseInt(
            dto.isSuccess() ? PayStatusEnum.SUCCESS.getCode() : PayStatusEnum.FAILED.getCode()));
        po.setChannelResponse(dto.getChannelResponse());
        po.setPaidAt(LocalDateTime.now());
        payManager.updateById(po);

        // ponytail: 同步 Feign 触发 order.pay，失败走 fallback 仅记日志；事务消息升级路径见 RocketMqEventBus。
        if (dto.isSuccess() && po.getOrderNo() != null) {
            orderFeignClient.pay(Long.valueOf(po.getOrderNo()));
        }
        log.info("payment callback handled, paymentNo={}, success={}", dto.getPaymentNo(), dto.isSuccess());
    }

    private String generatePaymentNo() {
        return PAYMENT_NO_PREFIX + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 创建退款。成功后通过 Feign 通知 order 服务推进退款状态。
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

        PayRefundPO refund = new PayRefundPO();
        refund.setRefundNo(refundNo);
        refund.setPaymentNo(paymentNo);
        refund.setAmount(dto.getAmount());
        refund.setReason(dto.getReason());
        refund.setStatus(Integer.parseInt(RefundStatusEnum.PENDING.getCode()));
        refund.setIdempotencyKey(UUID.randomUUID().toString());
        payRefundMapper.insert(refund);

        payTx.setStatus(Integer.parseInt(PayStatusEnum.REFUNDING.getCode()));
        payManager.updateById(payTx);

        Long orderId = Long.valueOf(payTx.getOrderNo());
        try {
            String refundTradeNo = alipayGateway.refund(paymentNo, refundNo, dto.getAmount(), dto.getReason());
            refund.setStatus(Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()));
            refund.setRefundTradeNo(refundTradeNo);
            refund.setNotifiedAt(LocalDateTime.now());
            payRefundMapper.updateById(refund);

            payTx.setStatus(Integer.parseInt(PayStatusEnum.REFUNDED.getCode()));
            payManager.updateById(payTx);

            orderFeignClient.refundCallback(orderId, true);
            log.info("refund success, refundNo={}, paymentNo={}", refundNo, paymentNo);
        }
        catch (Exception e) {
            refund.setStatus(Integer.parseInt(RefundStatusEnum.FAILED.getCode()));
            payRefundMapper.updateById(refund);
            payTx.setStatus(Integer.parseInt(PayStatusEnum.SUCCESS.getCode()));
            payManager.updateById(payTx);
            orderFeignClient.refundCallback(orderId, false);
            throw new BizException(PayCodeEnum.REFUND_FAILED);
        }

        return new RefundVO(refundNo, paymentNo, dto.getAmount(), refund.getStatus());
    }

    private String generateRefundNo() {
        return "REFUND" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 按订单号查询支付流水，不存在时抛出 PAY_NOT_FOUND。
     * @param orderNo 订单号
     * @return 支付流水实体
     */
    @Override
    public PayTransactionPO getByOrderNo(String orderNo) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getOrderNo, orderNo));
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }
        return po;
    }

    /**
     * 按支付单号查询支付参数，供前端调起渠道 SDK，不存在时抛出 PAY_NOT_FOUND。
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
        return new PaymentParamsVO(
            po.getPaymentNo(), po.getOrderNo(), po.getAmount(), po.getCurrency(),
            po.getChannel(), "Order " + po.getOrderNo(), po.getExpireAt());
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
     * 商家提现申请，生成提现单号并保存为 PENDING 状态。
     * @param merchantId 商家ID，由可信 Header 注入
     * @param dto 提现申请DTO
     * @return 提现单VO
     */
    @Override
    public WithdrawVO applyWithdraw(Long merchantId, WithdrawApplyDTO dto) {
        String withdrawNo = generateWithdrawNo();
        LocalDateTime appliedAt = LocalDateTime.now();

        MerchantWithdrawPO po = new MerchantWithdrawPO();
        po.setMerchantId(merchantId);
        po.setWithdrawNo(withdrawNo);
        po.setAmount(dto.getAmount());
        po.setStatus(Integer.parseInt(WithdrawStatusEnum.PENDING.getCode()));
        po.setReason(dto.getReason());
        po.setAppliedAt(appliedAt);
        merchantWithdrawManager.save(po);

        log.info("withdraw applied, withdrawNo={}, merchantId={}, amount={}",
            withdrawNo, merchantId, dto.getAmount());
        return new WithdrawVO(withdrawNo, merchantId, dto.getAmount(),
            Integer.parseInt(WithdrawStatusEnum.PENDING.getCode()), dto.getReason(), appliedAt, null);
    }

    private String generateWithdrawNo() {
        return WITHDRAW_NO_PREFIX + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    /**
     * 商家提现记录游标分页查询，merchantId 强制绑定，支持按状态与时间范围过滤。
     * @param merchantId 商家ID
     * @param dto 游标分页查询入参
     * @return 提现单游标分页结果
     */
    @Override
    public CursorPageVO<MerchantWithdrawPO> pageWithdraw(Long merchantId, PayPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<MerchantWithdrawPO> records = merchantWithdrawManager.list(
            Wrappers.lambdaQuery(MerchantWithdrawPO.class)
                .lt(lastId != null, MerchantWithdrawPO::getId, lastId)
                .eq(MerchantWithdrawPO::getMerchantId, merchantId)
                .eq(dto.getStatus() != null, MerchantWithdrawPO::getStatus, dto.getStatus())
                .ge(dto.getStartTime() != null, MerchantWithdrawPO::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, MerchantWithdrawPO::getCreateTime, dto.getEndTime())
                .orderByDesc(MerchantWithdrawPO::getId)
                .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, MerchantWithdrawPO::getId);
    }

    /**
     * 平台提现记录游标分页查询，不绑定 merchantId。
     * @param dto 游标分页查询入参
     * @return 提现单游标分页结果
     */
    @Override
    public CursorPageVO<MerchantWithdrawPO> platformPageWithdraw(PayPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<MerchantWithdrawPO> records = merchantWithdrawManager.list(
            Wrappers.lambdaQuery(MerchantWithdrawPO.class)
                .lt(lastId != null, MerchantWithdrawPO::getId, lastId)
                .eq(dto.getStatus() != null, MerchantWithdrawPO::getStatus, dto.getStatus())
                .ge(dto.getStartTime() != null, MerchantWithdrawPO::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, MerchantWithdrawPO::getCreateTime, dto.getEndTime())
                .orderByDesc(MerchantWithdrawPO::getId)
                .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, MerchantWithdrawPO::getId);
    }

    /**
     * 平台审核提现申请，approved=true 时状态置为 PAID，approved=false 时状态置为 REJECTED 并记录原因。
     * @param withdrawId 提现单ID
     * @param approved 是否通过
     * @param reason 驳回原因，approved=false 时填写
     */
    @Override
    public void reviewWithdraw(Long withdrawId, boolean approved, String reason) {
        MerchantWithdrawPO po = merchantWithdrawManager.getById(withdrawId);
        if (po == null) {
            throw new BizException(PayCodeEnum.WITHDRAW_NOT_FOUND);
        }

        if (approved) {
            po.setStatus(Integer.parseInt(WithdrawStatusEnum.PAID.getCode()));
        }
        else {
            po.setStatus(Integer.parseInt(WithdrawStatusEnum.REJECTED.getCode()));
            po.setReason(reason);
        }
        po.setReviewedAt(LocalDateTime.now());
        merchantWithdrawManager.updateById(po);
        log.info("withdraw reviewed, withdrawId={}, approved={}", withdrawId, approved);
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
}