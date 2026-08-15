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
    /** 支付宝当面付二维码内容（qr_code），前端生成二维码图片供用户扫码支付 */
    private String qrCode;
}
