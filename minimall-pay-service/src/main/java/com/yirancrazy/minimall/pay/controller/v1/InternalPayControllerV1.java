package com.yirancrazy.minimall.pay.controller.v1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.service.PayService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付内部控制器，提供Pay相关内部接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@RestController
@RequestMapping("/internal/pay")
public class InternalPayControllerV1 {

    private final PayService payService;

    public InternalPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付单并返回支付单标识。
     *
     * @param dto 支付单创建参数
     * @return 支付单标识
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto) {
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), dto.getUserId(), dto.getMerchantId(), dto.getAmount(), dto.getChannel());
        return Result.success(paymentId);
    }

    /**
     * 模拟支付回调入口，供订单服务内部调用以推进支付单状态。
     *
     * @param dto 支付回调请求参数
     * @return 回调处理是否成功
     */
    @PostMapping("/callback")
    public Result<Boolean> callback(@Valid @RequestBody PayCallbackDTO dto) {
        payService.handleCallback(dto);
        return Result.success(true);
    }

    /**
     * Create a refund for an existing payment and notify the order service
     * of the result so it can advance its status.
     * 幂等键取网关注入的 X-Idempotency-Key，防内部重复退款重放。
     *
     * @param dto 退款创建参数
     * @return 退款是否成功
     */
    @PostMapping("/refund")
    @Idempotent
    public Result<Boolean> refund(@Valid @RequestBody RefundCreateDTO dto) {
        payService.createRefund(dto);
        return Result.success(true);
    }

    /**
     * 全平台支付流水导出列表，按创建时间倒序，服务端限制最大导出行数。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param startDate 起始日期（yyyy-MM-dd），null 表示不限制
     * @param endDate 结束日期（yyyy-MM-dd），null 表示不限制
     * @return 支付流水导出项列表
     */
    @GetMapping("/export-transactions")
    public Result<List<PayExportItemDTO>> exportTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PayPageDTO dto = new PayPageDTO();
        dto.setStartTime(parseStartDate(startDate));
        dto.setEndTime(parseEndDate(endDate));
        return Result.success(payService.exportTransactions(dto).stream().map(this::toExportItem).toList());
    }

    /**
     * 起始日期 → 当日零点。
     * @param date 日期字符串（yyyy-MM-dd），空返回 null
     * @return 当日零点时间
     */
    private LocalDateTime parseStartDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date).atStartOfDay();
    }

    /**
     * 结束日期 → 当日最后一毫秒。
     * @param date 日期字符串（yyyy-MM-dd），空返回 null
     * @return 当日末刻时间
     */
    private LocalDateTime parseEndDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date).atTime(LocalTime.MAX);
    }

    /**
     * 支付流水PO → 导出项DTO。
     * @param po 支付流水持久化对象
     * @return 支付流水导出项DTO
     */
    private PayExportItemDTO toExportItem(PayTransactionPO po) {
        return new PayExportItemDTO(po.getPaymentNo(), po.getOrderNo(), po.getUserId(), po.getMerchantId(),
            po.getAmount(), po.getChannel(), po.getStatus(), po.getCreateTime());
    }
}