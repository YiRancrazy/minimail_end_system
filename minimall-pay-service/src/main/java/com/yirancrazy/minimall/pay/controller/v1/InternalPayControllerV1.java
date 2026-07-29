package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * 模拟支付回调入口，供订单服务内部调用以推进支付单状态。
     *
     * @param dto 支付回调请求参数，包含支付单主键 ID
     * @return 回调处理是否成功
     */
    @PostMapping("/callback")
    public Result<Boolean> callback(@Valid @RequestBody PayCallbackDTO dto) {
        return Result.success(payService.callback(dto.getPayId(), true));
    }
}