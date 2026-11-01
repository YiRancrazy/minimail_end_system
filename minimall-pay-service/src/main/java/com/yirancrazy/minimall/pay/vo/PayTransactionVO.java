package com.yirancrazy.minimall.pay.vo;

import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 返回可对外展示的支付流水字段。
 * 作为数据库实体与接口之间的边界，只暴露八个业务字段并隐藏渠道响应等内部信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class PayTransactionVo {

    private String paymentNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private Integer status;
    private Integer channel;
    private LocalDateTime createTime;

    /**
     * 将支付流水实体复制为对外视图，并排除渠道响应等内部字段。
     *
     * @param po 支付流水实体
     * @return 包含八个业务字段的支付流水视图
     */
    public static PayTransactionVo from(PayTransactionPo po) {
        PayTransactionVo vo = new PayTransactionVo();
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
