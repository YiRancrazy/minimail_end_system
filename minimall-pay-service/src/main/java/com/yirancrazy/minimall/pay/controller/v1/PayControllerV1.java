package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.service.PayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pay")
public class PayControllerV1 {

    private final PayService payService;

    public PayControllerV1(PayService payService) {
        this.payService = payService;
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody PayCreateDTO dto) {
        return Result.success(payService.create(dto.getOrderId(), dto.getAmount()));
    }
}