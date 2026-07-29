package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.service.PayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付 C 端接口控制器，对外暴露创建支付单等支付能力，前端发起支付前调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@RestController
@RequestMapping("/api/v1/pay")
public class PayControllerV1 {

    private final PayService payService;

    public PayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付单。
     *
     * @param dto 创建支付单请求参数，包含订单号与金额
     * @return 新建支付单的主键 ID
     */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody PayCreateDTO dto) {
        return Result.success(payService.create(dto.getOrderId(), dto.getAmount()));
    }
}