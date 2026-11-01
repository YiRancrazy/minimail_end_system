package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDto;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供订单服务调用的内部支付接口。
 * 覆盖支付单创建、支付回调、退款和导出；依赖 `PayService`，不负责用户身份鉴权。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@RestController
@RequestMapping("/internal/pay")
public class InternalPayControllerV1 {

    private final PayService payService;

    /**
     * 注入内部支付接口所需的支付服务。
     *
     * @param payService 负责创建、回调和退款编排的支付服务
     */
    public InternalPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建内部支付单，并以订单号作为 30 秒幂等键。
     * 用户、商家和金额由调用方传入，最终仍由服务层通过订单快照校验。
     *
     * @param dto 内部创建支付请求
     * @return 新建或幂等复用的支付单主键
     */
    @PostMapping("/create")
    @Idempotent(key = "#dto.orderNo", expire = 30, unit = TimeUnit.SECONDS)
    public Result<Long> create(@Valid @RequestBody PayCreateDto dto) {
        Long paymentId =
                payService.createPayment(
                        dto.getOrderNo(),
                        dto.getUserId(),
                        dto.getMerchantId(),
                        dto.getAmount(),
                        dto.getChannel());
        return Result.success(paymentId);
    }

    /**
     * 接收内部服务转发的支付结果并推进支付状态。
     * 成功受理后固定返回 true；业务校验失败由全局异常处理返回错误。
     *
     * @param dto 归一化支付回调
     * @return 回调处理完成时返回成功标记
     */
    @PostMapping("/callback")
    public Result<Boolean> callback(@Valid @RequestBody PayCallbackDto dto) {
        payService.handleCallback(dto);
        return Result.success(true);
    }

    /**
     * 按退款请求号幂等创建或恢复退款。
     *
     * @param dto 内部退款请求
     * @return 退款编排成功提交时返回成功标记
     */
    @PostMapping("/refund")
    @Idempotent(key = "#dto.refundRequestNo")
    public Result<Boolean> refund(@Valid @RequestBody RefundCreateDto dto) {
        payService.createRefund(dto);
        return Result.success(true);
    }

    /**
     * 按可选起止日期导出支付流水。
     * 日期格式非法时由服务层忽略对应边界，结果再转换为内部导出 DTO。
     *
     * @param startDate 起始日期，格式为 ISO-8601 日期，可为空
     * @param endDate 结束日期，格式为 ISO-8601 日期，可为空
     * @return 最多包含导出上限数量的支付流水
     */
    @GetMapping("/export-transactions")
    public Result<List<PayExportItemDto>> exportTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(
                payService.exportTransactionVos(startDate, endDate).stream()
                        .map(this::toExportItem)
                        .toList());
    }

    /**
     * 将支付视图转换为内部导出契约，避免把本模块 VO 暴露给调用方。
     *
     * @param vo 支付流水视图
     * @return 字段一一对应的导出项
     */
    private PayExportItemDto toExportItem(PayTransactionVo vo) {
        return new PayExportItemDto(
                vo.getPaymentNo(),
                vo.getOrderNo(),
                vo.getUserId(),
                vo.getMerchantId(),
                vo.getAmount(),
                vo.getChannel(),
                vo.getStatus(),
                vo.getCreateTime());
    }
}
