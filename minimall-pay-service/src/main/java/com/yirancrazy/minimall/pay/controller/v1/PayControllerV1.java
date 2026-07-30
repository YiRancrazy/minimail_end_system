package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.RefundCreateDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.RefundVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/pay")
public class PayControllerV1 {

    private final PayService payService;

    public PayControllerV1(PayService payService) {
        this.payService = payService;
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestParam String orderNo,
                                @RequestParam Long userId,
                                @RequestParam Long merchantId,
                                @RequestParam BigDecimal amount) {
        Long paymentId = payService.createPayment(orderNo, userId, merchantId, amount);
        return Result.success(paymentId);
    }

    @PostMapping("/callback/alipay")
    public String callback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));

        try {
            String tradeNo = params.get("trade_no");
            String paymentNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

            PayCallbackDTO dto = new PayCallbackDTO(paymentNo, tradeNo, success, params.toString());
            payService.handleCallback(dto);
            return "success";
        } catch (BizException e) {
            log.error("callback failed: {}", e.getMessage());
            return "fail";
        }
    }

    @PostMapping("/refunds")
    public Result<RefundVO> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        RefundVO vo = payService.createRefund(dto);
        return Result.success(vo);
    }
}