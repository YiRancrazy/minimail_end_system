package com.yirancrazy.minimall.pay.controller.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载支付宝电脑网站支付的浏览器同步返回页。
 * 仅展示并转义回传参数，不推进支付或订单状态；异步状态变更由 Alipay 回调接口负责。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pay")
public class PayReturnControllerV1 {

  /**
   * 渲染支付宝同步返回的支付成功页。
   * 页面只转义展示支付单号、支付宝交易号和金额；支付状态不会在此接口改变。
   *
   * @param outTradeNo 支付单号，可为空
   * @param tradeNo 支付宝交易号，可为空
   * @param totalAmount 支付金额文本，可为空
   * @return UTF-8 编码的支付成功 HTML
   */
  @GetMapping(value = "/success", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
  public String success(
      @RequestParam(value = "out_trade_no", required = false) String outTradeNo,
      @RequestParam(value = "trade_no", required = false) String tradeNo,
      @RequestParam(value = "total_amount", required = false) String totalAmount) {
    log.info("支付宝同步返回成功页已访问，paymentNo={}，tradeNo={}，amount={}", outTradeNo, tradeNo, totalAmount);
    return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><meta"
               + " name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
               + "<title>支付成功</title></head><body"
               + " style=\"margin:0;display:flex;align-items:center;justify-content:center;"
               + "min-height:100vh;"
               + "background:#f5f7fa;font-family:sans-serif;\"><div"
               + " style=\"text-align:center;background:#fff;padding:2rem"
               + " 3rem;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.08);\"><h2"
               + " style=\"color:#07c160;margin:0 0 1rem;\">支付成功</h2><p"
               + " style=\"color:#666;margin:.4rem 0;\">支付单号："
        + escape(outTradeNo)
        + "</p>"
        + "<p style=\"color:#666;margin:.4rem 0;\">支付金额：¥"
        + escape(totalAmount)
        + "</p>"
        + "<p style=\"color:#999;margin:1rem 0 0;\">请返回应用查看订单</p>"
        + "</div></body></html>";
  }

  /**
   * 转义插入 HTML 文本节点的特殊字符，防止同步返回参数形成脚本注入。
   *
   * @param value 待展示的请求参数
   * @return 转义后的文本；输入为 null 时返回空字符串
   */
  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
