package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 返回用户拉起支付宝收银台所需的信息。
 * 包含支付单主体、金额、渠道、过期时间及签名后的页面表单，不保存额外业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentParamsVo {
    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private String currency;
    private Integer channel;
    private String subject;
    private LocalDateTime expireAt;

    private String payForm;
}
