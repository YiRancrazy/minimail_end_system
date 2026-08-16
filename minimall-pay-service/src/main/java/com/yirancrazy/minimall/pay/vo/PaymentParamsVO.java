package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付参数视图对象，前端据此调起对应渠道 SDK。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentParamsVO {
    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private String currency;
    private Integer channel;
    private String subject;
    private LocalDateTime expireAt;
    /** 支付宝电脑网站支付表单 HTML（page.pay），前端写入当前页后自动跳转支付宝收银台 */
    private String payForm;
}
