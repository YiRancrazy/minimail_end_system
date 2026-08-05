package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.WithdrawPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.merchant.service.WithdrawService;
import com.yirancrazy.minimall.merchant.vo.WithdrawVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端提现审核控制器，提供提现申请分页查询与通过/驳回操作。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/platform/withdraws")
public class PlatformWithdrawControllerV1 {

    private final WithdrawService withdrawService;

    public PlatformWithdrawControllerV1(WithdrawService withdrawService) {
        this.withdrawService = withdrawService;
    }

    /**
     * 平台分页查询全部提现申请，支持按商家ID与状态过滤。
     * @param dto 分页入参
     * @return 提现申请分页结果
     */
    @GetMapping
    public Result<CursorPageVO<WithdrawVO>> page(@Valid WithdrawPageDTO dto) {
        CursorPageVO<MerchantWithdrawPO> page = withdrawService.pageAll(dto);
        return Result.success(page.map(WithdrawVO::from));
    }

    /**
     * 平台审核通过，将 PENDING 置为 APPROVED。
     * @param id 提现申请ID
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        withdrawService.approve(id);
        return Result.success(null);
    }

    /**
     * 平台审核驳回，将 PENDING 置为 REJECTED 并记录原因。
     * @param id 提现申请ID
     * @param reason 驳回原因
     */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam String reason) {
        withdrawService.reject(id, reason);
        return Result.success(null);
    }
}
