package com.yirancrazy.minimall.pay.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantPayControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/pay")
public class MerchantPayControllerV1 {

    private final PayService payService;

    /**
     * 构造 MerchantPayControllerV1 实例。
     * @param payService payService 参数
     */
    public MerchantPayControllerV1(PayService payService) {
        this.payService = payService;
    }


    /**
     * 执行 transactions 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/transactions")
    public Result<CursorPageVO<PayTransactionVO>> transactions(
        @RequestHeader("X-Merchant-Id") Long merchantId, @Valid PayPageDTO dto) {
        return Result.success(payService.pageVO(merchantId, dto));
    }


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/statistics")
    public Result<PayStatisticsVO> statistics(@RequestHeader("X-Merchant-Id") Long merchantId,
                                              @Valid PayPageDTO dto) {
        return Result.success(payService.statistics(merchantId, dto));
    }
}
