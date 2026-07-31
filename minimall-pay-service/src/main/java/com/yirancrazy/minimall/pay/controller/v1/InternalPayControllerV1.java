package com.yirancrazy.minimall.pay.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.service.PayService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付内部控制器，提供Pay相关内部接口
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class InternalPayControllerV1 {

    private final PayService payService;

    public InternalPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付单并返回支付单标识。
     *
     * @param dto 支付单创建参数
     * @return 支付单标识
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto) {
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), dto.getUserId(), dto.getMerchantId(), dto.getAmount());
        return Result.success(paymentId);
    }

    /**
     * 模拟支付回调入口，供订单服务内部调用以推进支付单状态。
     *
     * @param dto 支付回调请求参数
     * @return 回调处理是否成功
     */
    @PostMapping("/callback")
    public Result<Boolean> callback(@Valid @RequestBody PayCallbackDTO dto) {
        payService.handleCallback(dto);
        return Result.success(true);
    }
}