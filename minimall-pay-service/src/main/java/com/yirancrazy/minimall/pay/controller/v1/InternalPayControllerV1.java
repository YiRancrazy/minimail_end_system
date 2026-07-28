package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.service.PayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 伪支付宝异步回调。真实场景由支付宝 POST 异步通知，此处由 order-service
 * 同步内部调用以推进支付单状态。
 */
@RestController
@RequestMapping("/internal/pay")
public class InternalPayControllerV1 {

    private final PayService payService;

    public InternalPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    @PostMapping("/callback")
    public Result<Boolean> callback(@RequestParam("payId") Long payId) {
        return Result.success(payService.callback(payId, true));
    }
}