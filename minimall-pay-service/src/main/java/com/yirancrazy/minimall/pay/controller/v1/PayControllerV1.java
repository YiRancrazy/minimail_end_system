package com.yirancrazy.minimall.pay.controller.v1;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCreateDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayControllerV1 类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/pay")
public class PayControllerV1 {

    private final PayService payService;

    public PayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付流水，channel 为空时默认 ALIPAY。
     * @param dto 支付流水创建DTO
     * @return 支付流水ID
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto) {
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), dto.getUserId(), dto.getMerchantId(), dto.getAmount(), dto.getChannel());
        return Result.success(paymentId);
    }

    /**
     * 处理支付宝回调。
     * @param request HTTP请求
     * @return 回调结果
     */
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
        }
        catch (BizException e) {
            log.error("callback failed: {}", e.getMessage());
            return "fail";
        }
    }

    /**
     * 创建退款。
     * @param dto 退款创建DTO
     * @return 退款VO
     */
    @PostMapping("/refunds")
    public Result<RefundVO> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        RefundVO vo = payService.createRefund(dto);
        return Result.success(vo);
    }

    /**
     * 按订单号查询支付流水状态。
     * @param orderNo 订单号
     * @return 支付流水实体
     */
    @GetMapping("/status/{orderNo}")
    public Result<PayTransactionPO> getStatus(@PathVariable("orderNo") String orderNo) {
        return Result.success(payService.getByOrderNo(orderNo));
    }

    /**
     * 按支付单号查询支付参数，供前端调起渠道 SDK。
     * @param paymentNo 支付单号
     * @return 支付参数VO
     */
    @GetMapping("/params/{paymentNo}")
    public Result<PaymentParamsVO> getParams(@PathVariable("paymentNo") String paymentNo) {
        return Result.success(payService.getPaymentParams(paymentNo));
    }
}