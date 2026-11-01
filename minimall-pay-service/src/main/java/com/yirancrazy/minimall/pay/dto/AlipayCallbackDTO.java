package com.yirancrazy.minimall.pay.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装支付宝回调的原始参数映射。
 * 提供 null-safe 字段读取，供验签、交易状态和金额解析使用；本身不修改支付状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlipayCallbackDto {

    private Map<String, String> params;

    /**
     * 从支付宝原始参数中读取商户支付单号。
     *
     * @return `out_trade_no`；参数缺失时返回 null
     */
    public String getOutTradeNo() {
        return params == null ? null : params.get("out_trade_no");
    }

    /**
     * 从支付宝原始参数中读取交易状态，用于判断成功、关闭或待处理。
     *
     * @return `trade_status`；参数缺失时返回 null
     */
    public String getTradeStatus() {
        return params == null ? null : params.get("trade_status");
    }

    /**
     * 从支付宝原始参数中读取回调金额文本。
     *
     * @return `total_amount`；参数缺失时返回 null
     */
    public String getTotalAmount() {
        return params == null ? null : params.get("total_amount");
    }
}
