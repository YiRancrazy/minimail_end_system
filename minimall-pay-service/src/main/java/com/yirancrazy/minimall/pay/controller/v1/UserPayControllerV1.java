package com.yirancrazy.minimall.pay.controller.v1;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCreateDTO;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端支付控制器，提供创建支付流水、支付宝回调、退款、状态查询与支付参数获取。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/user/pay")
public class UserPayControllerV1 {

    private final PayService payService;
    private final PayGateway payGateway;

    public UserPayControllerV1(PayService payService, PayGateway payGateway) {
        this.payService = payService;
        this.payGateway = payGateway;
    }

    /**
     * 创建支付流水，channel 为空时默认 ALIPAY。
     * 同订单待支付/已成功流水由 service 层复用（防重复扣款），失败/关闭流水允许重新创建，故不加 @Idempotent。
     * @param dto 支付流水创建DTO
     * @return 支付流水ID
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        Integer channelCode = dto.getChannel() == null ? null : Integer.parseInt(dto.getChannel().getCode());
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), userId, merchantId, dto.getAmount(), channelCode);
        return Result.success(paymentId);
    }

    /**
     * 处理支付宝异步回调，先验签再推进业务状态，验签失败返回 fail 由支付宝重试。
     * @param request HTTP请求
     * @return 回调结果
     */
    @PostMapping("/callback/alipay")
    public String callback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        log.info("alipay callback received, paymentNo={}, tradeStatus={}, paramsSize={}",
            params.get("out_trade_no"), params.get("trade_status"), params.size());

        try {
            String tradeNo = payGateway.verifyCallback(params);
            String paymentNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

            PayCallbackDTO dto = new PayCallbackDTO(paymentNo, tradeNo, success, params.toString());
            payService.handleCallback(dto);
            log.info("alipay callback handled, paymentNo={}, tradeNo={}, success={}", paymentNo, tradeNo, success);
            return "success";
        }
        catch (BizException e) {
            log.error("callback failed: {}", e.getMessage());
            return "fail";
        }
        catch (Exception e) {
            log.error("callback verify failed", e);
            return "fail";
        }
    }

    /**
     * 创建退款。
     * @param dto 退款创建DTO
     * @return 退款VO
     */
    @PostMapping("/refunds")
    @Idempotent
    public Result<RefundVO> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        RefundVO vo = payService.createRefund(dto);
        return Result.success(vo);
    }

    /**
     * 按订单号查询支付流水状态。
     * @param orderNo 订单号
     * @return 支付流水VO
     */
    @GetMapping("/status/{orderNo}")
    public Result<PayTransactionVO> getStatus(@PathVariable("orderNo") String orderNo) {
        return Result.success(PayTransactionVO.from(payService.getByOrderNo(orderNo)));
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
