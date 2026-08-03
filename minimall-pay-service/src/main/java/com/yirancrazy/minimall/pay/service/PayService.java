package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务接口，定义核心业务逻辑。
 * @Version: 1.1
 * @DateTime: 2026/7/31
 **/
public interface PayService {
    /**
     * 创建支付流水，channel 为空时默认 ALIPAY，不支持渠道抛出 PAY_CHANNEL_UNSUPPORTED。
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param merchantId 商户ID
     * @param amount 支付金额
     * @param channel 支付渠道 code，null 默认 ALIPAY
     * @return 支付流水ID
     */
    Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel);

    /**
     * 处理支付回调。
     * @param dto 支付回调DTO
     */
    void handleCallback(PayCallbackDTO dto);

    /**
     * 创建退款。
     * @param dto 退款创建DTO
     * @return 退款VO
     */
    RefundVO createRefund(RefundCreateDTO dto);

    /**
     * 按订单号查询支付流水，不存在时抛出 PAY_NOT_FOUND。
     * @param orderNo 订单号
     * @return 支付流水实体
     */
    PayTransactionPO getByOrderNo(String orderNo);

    /**
     * 按支付单号查询支付参数，供前端调起渠道 SDK，不存在时抛出 PAY_NOT_FOUND。
     * @param paymentNo 支付单号
     * @return 支付参数VO
     */
    PaymentParamsVO getPaymentParams(String paymentNo);

    /**
     * 商家资金流水分页查询，merchantId 强制绑定，支持按状态与时间范围过滤。
     * @param merchantId 商家ID
     * @param dto 分页查询入参
     * @return 支付流水分页结果
     */
    IPage<PayTransactionPO> page(Long merchantId, PayPageDTO dto);

    /**
     * 平台全平台交易流水分页查询，不绑定 merchantId。
     * @param dto 分页查询入参
     * @return 支付流水分页结果
     */
    IPage<PayTransactionPO> platformPage(PayPageDTO dto);

    /**
     * 资金统计聚合查询，merchantId 为空时统计全平台。
     * @param merchantId 商家ID，null 表示全平台
     * @param dto 分页查询入参（复用时间范围字段）
     * @return 统计VO
     */
    PayStatisticsVO statistics(Long merchantId, PayPageDTO dto);

    /**
     * 异常支付冻结，将支付单状态置为 FROZEN，不存在时抛出 PAY_NOT_FOUND。
     * @param paymentNo 支付单号
     */
    void freeze(String paymentNo);
}