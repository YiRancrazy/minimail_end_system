package com.yirancrazy.minimall.pay.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.constant.PayStatusEnum;
import com.yirancrazy.minimall.pay.constant.RefundStatusEnum;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.service.PayService;
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

    private final PayManager payManager;
    private final AlipayGateway alipayGateway;
    private final PayRefundMapper payRefundMapper;
    private final OrderFeignClient orderFeignClient;

    public PayServiceImpl(PayManager payManager, AlipayGateway alipayGateway,
                          PayRefundMapper payRefundMapper, OrderFeignClient orderFeignClient) {
        this.payManager = payManager;
        this.alipayGateway = alipayGateway;
        this.payRefundMapper = payRefundMapper;
        this.orderFeignClient = orderFeignClient;
    }

    /**
     * 创建支付流水。
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param merchantId 商户ID
     * @param amount 支付金额
     * @return 支付流水ID
     */
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
}