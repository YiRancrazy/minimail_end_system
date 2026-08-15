package com.yirancrazy.minimall.pay.controller.v1;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付宝同步跳转落地页：支付完成后浏览器被重定向至此，业务状态以异步回调为准，本页仅展示结果。
 * @Version: 1.0
 * @DateTime: 2026/08/15
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/pay")
public class PayReturnControllerV1 {

    /**
     * 支付宝支付成功后浏览器同步跳转的落地页，展示支付结果并引导返回应用。
     * @param outTradeNo 支付单号
     * @param tradeNo 支付宝交易号
     * @param totalAmount 支付金额
     * @return 支付结果 HTML 页面
     */
    @GetMapping(value = "/success", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String success(@RequestParam(value = "out_trade_no", required = false) String outTradeNo,
                          @RequestParam(value = "trade_no", required = false) String tradeNo,
                          @RequestParam(value = "total_amount", required = false) String totalAmount) {
        log.info("alipay return success page visited, paymentNo={}, tradeNo={}, amount={}",
            outTradeNo, tradeNo, totalAmount);
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
            + "<title>支付成功</title></head>"
            + "<body style=\"margin:0;display:flex;align-items:center;justify-content:center;min-height:100vh;"
            + "background:#f5f7fa;font-family:sans-serif;\">"
            + "<div style=\"text-align:center;background:#fff;padding:2rem 3rem;border-radius:12px;"
            + "box-shadow:0 2px 8px rgba(0,0,0,.08);\">"
            + "<h2 style=\"color:#07c160;margin:0 0 1rem;\">支付成功</h2>"
            + "<p style=\"color:#666;margin:.4rem 0;\">支付单号：" + escape(outTradeNo) + "</p>"
            + "<p style=\"color:#666;margin:.4rem 0;\">支付金额：¥" + escape(totalAmount) + "</p>"
            + "<p style=\"color:#999;margin:1rem 0 0;\">请返回应用查看订单</p>"
            + "</div></body></html>";
    }

    /**
     * 转义 HTML 特殊字符，防止支付宝跳转参数注入脚本。
     * @param value 原始参数
     * @return 转义后的字符串
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
