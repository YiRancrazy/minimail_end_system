package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayTransactionVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
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
