package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDto;
import com.yirancrazy.minimall.pay.dto.PayCreateDto;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户支付单创建、状态查询、收银台参数和支付宝异步回调接口。
 * 用户身份来自 `X-User-Id`，支付归属和金额仍由 `PayService` 使用订单快照校验。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/pay")
public class UserPayControllerV1 {

    private final PayService payService;

    /**
     * 注入用户支付业务服务。
     *
     * @param payService 用户支付创建、查询和回调服务
     */
    public UserPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 以订单号作为 30 秒幂等键创建用户支付单。
     * 用户身份取自可信请求头，商家字段仅作为提示，实际归属由订单快照覆盖。
     *
     * @param dto 订单号、金额和渠道
     * @param userId 当前用户 ID
     * @param merchantId 可选商家 ID
     * @return 新建或复用的支付单主键
     */
    @PostMapping("/create")
    @Idempotent(key = "#dto.orderNo", expire = 30, unit = TimeUnit.SECONDS)
    public Result<Long> create(
            @Valid @RequestBody PayCreateDto dto,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        Integer channelCode =
                dto.getChannel() == null ? null : Integer.parseInt(dto.getChannel().getCode());
        Long paymentId =
                payService.createPayment(
                        dto.getOrderNo(), userId, merchantId, dto.getAmount(), channelCode);
        return Result.success(paymentId);
    }

    /**
     * 接收支付宝异步通知并交由服务层验签和处理。
     * 仅当服务返回 true 时响应 `success`，否则响应 `fail` 以触发支付宝重试。
     *
     * @param request 支付宝表单请求
     * @return 支付宝要求的 `success` 或 `fail` 文本
     */
    @PostMapping("/callback/alipay")
    public String callback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        return payService.handleAlipayCallback(new AlipayCallbackDto(params)) ? "success" : "fail";
    }

    /**
     * 查询当前用户指定订单号的最新支付状态。
     *
     * @param orderNo 订单号
     * @param userId 当前用户 ID
     * @return 经过归属校验的支付流水视图
     */
    @GetMapping("/status/{orderNo}")
    public Result<PayTransactionVo> getStatus(
            @PathVariable("orderNo") String orderNo, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(payService.getStatusVo(orderNo, userId));
    }

    /**
     * 获取当前用户指定支付单的支付宝收银台参数。
     * 服务层会校验支付单归属并实时生成签名表单。
     *
     * @param paymentNo 支付单号
     * @param userId 当前用户 ID
     * @return 收银台主体、金额、过期时间及支付表单
     */
    @GetMapping("/params/{paymentNo}")
    public Result<PaymentParamsVo> getParams(
            @PathVariable("paymentNo") String paymentNo, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(payService.getPaymentParams(paymentNo, userId));
    }
}
