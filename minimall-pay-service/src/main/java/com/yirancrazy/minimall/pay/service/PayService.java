package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.vo.RefundVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务接口，定义核心业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
public interface PayService {
    /**
     * 创建支付流水。
     * @param orderNo 订单号
     * @param userId 用户ID
     * @param merchantId 商户ID
     * @param amount 支付金额
     * @return 支付流水ID
     */
    Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount);

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
}