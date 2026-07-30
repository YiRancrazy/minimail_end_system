package com.yirancrazy.minimall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.RefundCreateDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.RefundVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class PayServiceImpl implements PayService {

    private static final String PAYMENT_NO_PREFIX = "PAY";
    private static final DateTimeFormatter EXPIRE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PayManager payManager;
    private final AlipayGateway alipayGateway;
    private final PayRefundMapper payRefundMapper;

    public PayServiceImpl(PayManager payManager, AlipayGateway alipayGateway, PayRefundMapper payRefundMapper) {
        this.payManager = payManager;
        this.alipayGateway = alipayGateway;
        this.payRefundMapper = payRefundMapper;
    }

    @Override
    public Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount) {
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
        po.setChannel(Integer.parseInt(PayChannelEnum.ALIPAY.getCode()));
        po.setExpireAt(expireAt);
        po.setIdempotencyKey(UUID.randomUUID().toString());
        payManager.save(po);

        String expireTime = expireAt.format(EXPIRE_FORMATTER);
        alipayGateway.createPayment(paymentNo, amount, "Order " + orderNo, expireTime);
        log.info("payment created, paymentNo={}, orderNo={}", paymentNo, orderNo);
        return po.getId();
    }

    @Override
    public void handleCallback(PayCallbackDTO dto) {
        PayTransactionPO po = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class).eq(PayTransactionPO::getPaymentNo, dto.getPaymentNo()));
        if (po == null) {
            throw new BizException(PayCodeEnum.PAY_NOT_FOUND);
        }

        po.setTradeNo(dto.getTradeNo());
        po.setStatus(Integer.parseInt(dto.isSuccess() ? PayStatusEnum.SUCCESS.getCode() : PayStatusEnum.FAILED.getCode()));
        po.setChannelResponse(dto.getChannelResponse());
        po.setPaidAt(LocalDateTime.now());
        payManager.updateById(po);

        log.info("payment callback handled, paymentNo={}, success={}", dto.getPaymentNo(), dto.isSuccess());
    }

    private String generatePaymentNo() {
        return PAYMENT_NO_PREFIX + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    @Override
    public RefundVO createRefund(RefundCreateDTO dto) {
        PayTransactionPO payTx = payManager.getOne(
            Wrappers.lambdaQuery(PayTransactionPO.class)
                .eq(PayTransactionPO::getPaymentNo, dto.getPaymentNo()));

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

        PayRefundPO refund = new PayRefundPO();
        refund.setRefundNo(refundNo);
        refund.setPaymentNo(dto.getPaymentNo());
        refund.setAmount(dto.getAmount());
        refund.setReason(dto.getReason());
        refund.setStatus(Integer.parseInt(RefundStatusEnum.PENDING.getCode()));
        refund.setIdempotencyKey(UUID.randomUUID().toString());
        payRefundMapper.insert(refund);

        payTx.setStatus(Integer.parseInt(PayStatusEnum.REFUNDING.getCode()));
        payManager.updateById(payTx);

        try {
            String refundTradeNo = alipayGateway.refund(dto.getPaymentNo(), refundNo, dto.getAmount(), dto.getReason());
            refund.setStatus(Integer.parseInt(RefundStatusEnum.SUCCESS.getCode()));
            refund.setRefundTradeNo(refundTradeNo);
            refund.setNotifiedAt(LocalDateTime.now());
            payRefundMapper.updateById(refund);

            payTx.setStatus(Integer.parseInt(PayStatusEnum.REFUNDED.getCode()));
            payManager.updateById(payTx);

            log.info("refund success, refundNo={}, paymentNo={}", refundNo, dto.getPaymentNo());
        } catch (Exception e) {
            refund.setStatus(Integer.parseInt(RefundStatusEnum.FAILED.getCode()));
            payRefundMapper.updateById(refund);
            payTx.setStatus(Integer.parseInt(PayStatusEnum.SUCCESS.getCode()));
            payManager.updateById(payTx);
            throw new BizException(PayCodeEnum.REFUND_FAILED);
        }

        return new RefundVO(refundNo, dto.getPaymentNo(), dto.getAmount(), refund.getStatus());
    }

    private String generateRefundNo() {
        return "REFUND" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}