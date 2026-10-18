package com.yirancrazy.minimall.pay.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AlipayCallbackDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlipayCallbackDTO {


    private Map<String, String> params;


    /**
     * 获取相关数据。
     * @return 处理结果
     */
    public String getOutTradeNo() {
        return params == null ? null : params.get("out_trade_no");
    }


    /**
     * 获取相关数据。
     * @return 处理结果
     */
    public String getTradeStatus() {
        return params == null ? null : params.get("trade_status");
    }


    /**
     * 获取相关数据。
     * @return 处理结果
     */
    public String getTotalAmount() {
        return params == null ? null : params.get("total_amount");
    }
}
