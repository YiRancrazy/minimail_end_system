package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.merchant.dto.WithdrawPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.merchant.service.WithdrawService;
import com.yirancrazy.minimall.merchant.vo.WithdrawVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端提现控制器，提供提现申请与自身申请分页查询。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant/withdraws")
public class MerchantWithdrawControllerV1 {

    private final WithdrawService withdrawService;

    public MerchantWithdrawControllerV1(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    /**
     * 商家发起提现申请，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 提现申请入参
     * @return 提现申请ID
     */
    @PostMapping
    public Result<Long> apply(@RequestHeader("X-Merchant-Id") Long merchantId,
                              @Valid @RequestBody WithdrawApplyDTO dto) {
        return Result.success(withdrawService.apply(merchantId, dto));
    }

    /**
     * 商家分页查询自身提现申请，按 ID 倒序游标分页。
     * @param merchantId 商家ID
     * @param dto 分页入参
     * @return 提现申请分页结果
     */
    @GetMapping
    public Result<CursorPageVO<WithdrawVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                 @Valid WithdrawPageDTO dto) {
        dto.setMerchantId(merchantId);
        CursorPageVO<MerchantWithdrawPO> page = withdrawService.pageByMerchant(dto);
        return Result.success(page.map(WithdrawVO::from));
    }
}
