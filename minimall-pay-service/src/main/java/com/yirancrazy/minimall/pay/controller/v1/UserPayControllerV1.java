package com.yirancrazy.minimall.pay.controller.v1;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCreateDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserPayControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/pay")
public class UserPayControllerV1 {

    private final PayService payService;

    /**
     * 构造 UserPayControllerV1 实例。
     * @param payService payService 参数
     */
    public UserPayControllerV1(PayService payService) {
        this.payService = payService;
    }


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @param userId userId 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @PostMapping("/create")
    @Idempotent(key = "#dto.orderNo", expire = 30, unit = TimeUnit.SECONDS)
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        Integer channelCode = dto.getChannel() == null ? null : Integer.parseInt(dto.getChannel().getCode());
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), userId, merchantId, dto.getAmount(), channelCode);
        return Result.success(paymentId);
    }


    /**
     * 执行 callback 方法。
     * @param request request 参数
     * @return 处理结果
     */
    @PostMapping("/callback/alipay")
    public String callback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));
        return payService.handleAlipayCallback(new AlipayCallbackDTO(params)) ? "success" : "fail";
    }


    /**
     * 获取相关数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/status/{orderNo}")
    public Result<PayTransactionVO> getStatus(@PathVariable("orderNo") String orderNo,
                                              @RequestHeader("X-User-Id") Long userId) {
        return Result.success(payService.getStatusVO(orderNo, userId));
    }


    /**
     * 获取相关数据。
     * @param paymentNo paymentNo 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/params/{paymentNo}")
    public Result<PaymentParamsVO> getParams(@PathVariable("paymentNo") String paymentNo,
                                             @RequestHeader("X-User-Id") Long userId) {
        return Result.success(payService.getPaymentParams(paymentNo, userId));
    }

}
