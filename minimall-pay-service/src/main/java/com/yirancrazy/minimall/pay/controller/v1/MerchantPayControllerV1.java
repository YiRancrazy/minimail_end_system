package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayPageDto;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVo;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供商家侧支付流水查询和统计接口。
 * 通过 `X-Merchant-Id` 限定数据范围，并将分页、状态和时间筛选委托给 `PayService`。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/pay")
public class MerchantPayControllerV1 {

    private final PayService payService;

    /**
     * 注入商家支付查询与统计服务。
     *
     * @param payService 提供商家维度的支付查询能力
     */
    public MerchantPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 分页查询指定商家的支付流水。
     * 商家范围来自可信请求头，状态和时间条件由分页 DTO 提供。
     *
     * @param merchantId 可信商家 ID
     * @param dto 游标、状态和时间筛选条件
     * @return 商家范围内的支付流水游标页
     */
    @GetMapping("/transactions")
    public Result<CursorPageVo<PayTransactionVo>> transactions(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid PayPageDto dto) {
        return Result.success(payService.pageVo(merchantId, dto));
    }

    /**
     * 统计指定商家的支付金额、退款金额和交易笔数。
     *
     * @param merchantId 可信商家 ID
     * @param dto 统计时间范围筛选条件
     * @return 商家支付统计视图
     */
    @GetMapping("/statistics")
    public Result<PayStatisticsVo> statistics(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid PayPageDto dto) {
        return Result.success(payService.statistics(merchantId, dto));
    }
}
