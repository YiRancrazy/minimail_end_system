package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付流水VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class PayTransactionVO {

    private String paymentNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private Integer status;
    private Integer channel;
    private LocalDateTime createTime;

    /**
     * 将 PayTransactionPO 转换为 PayTransactionVO。
     * @param po 支付流水持久化对象
     * @return 支付流水VO
     */
    public static PayTransactionVO from(PayTransactionPO po) {
        PayTransactionVO vo = new PayTransactionVO();
        vo.setPaymentNo(po.getPaymentNo());
        vo.setOrderNo(po.getOrderNo());
        vo.setUserId(po.getUserId());
        vo.setMerchantId(po.getMerchantId());
        vo.setAmount(po.getAmount());
        vo.setStatus(po.getStatus());
        vo.setChannel(po.getChannel());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
