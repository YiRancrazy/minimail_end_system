package com.yirancrazy.minimall.pay.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.WithdrawVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端支付控制器，提供商家资金流水分页、交易汇总统计、提现申请与提现记录查询。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@RestController
@RequestMapping("/api/v1/merchant/pay")
public class MerchantPayControllerV1 {

    private final PayService payService;

    public MerchantPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 商家资金流水游标分页查询，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 游标分页查询入参
     * @return 支付流水游标分页结果
     */
    @GetMapping("/transactions")
    public Result<CursorPageVO<PayTransactionVO>> transactions(
        @RequestHeader("X-Merchant-Id") Long merchantId, @Valid PayPageDTO dto) {
        return Result.success(payService.page(merchantId, dto).map(PayTransactionVO::from));
    }

    /**
     * 商家交易汇总统计，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 分页查询入参（复用时间范围字段）
     * @return 统计VO
     */
    @GetMapping("/statistics")
    public Result<PayStatisticsVO> statistics(@RequestHeader("X-Merchant-Id") Long merchantId,
                                              @Valid PayPageDTO dto) {
        return Result.success(payService.statistics(merchantId, dto));
    }

    /**
     * 商家提现申请，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 提现申请DTO
     * @return 提现单VO
     */
    @PostMapping("/withdraw")
    public Result<WithdrawVO> applyWithdraw(@RequestHeader("X-Merchant-Id") Long merchantId,
                                            @Valid @RequestBody WithdrawApplyDTO dto) {
        return Result.success(payService.applyWithdraw(merchantId, dto));
    }

    /**
     * 商家提现记录游标分页查询，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 游标分页查询入参
     * @return 提现单游标分页结果
     */
    @GetMapping("/withdrawals")
    public Result<CursorPageVO<MerchantWithdrawPO>> withdrawals(
        @RequestHeader("X-Merchant-Id") Long merchantId, @Valid PayPageDTO dto) {
        return Result.success(payService.pageWithdraw(merchantId, dto));
    }
}
