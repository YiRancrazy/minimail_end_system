package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现视图对象，用于返回提现单详情。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawVO {
    private String withdrawNo;
    private Long merchantId;
    private BigDecimal amount;
    private Integer status;
    private String reason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}
