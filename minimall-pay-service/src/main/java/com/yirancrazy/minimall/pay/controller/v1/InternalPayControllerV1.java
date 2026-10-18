package com.yirancrazy.minimall.pay.controller.v1;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalPayControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@RestController
@RequestMapping("/internal/pay")
public class InternalPayControllerV1 {

    private final PayService payService;

    /**
     * 构造 InternalPayControllerV1 实例。
     * @param payService payService 参数
     */
    public InternalPayControllerV1(PayService payService) {
        this.payService = payService;
    }


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/create")
    @Idempotent(key = "#dto.orderNo", expire = 30, unit = TimeUnit.SECONDS)
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto) {
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), dto.getUserId(), dto.getMerchantId(), dto.getAmount(), dto.getChannel());
        return Result.success(paymentId);
    }


    /**
     * 执行 callback 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/callback")
    public Result<Boolean> callback(@Valid @RequestBody PayCallbackDTO dto) {
        payService.handleCallback(dto);
        return Result.success(true);
    }


    /**
     * 执行 refund 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/refund")
    @Idempotent
    public Result<Boolean> refund(@Valid @RequestBody RefundCreateDTO dto) {
        payService.createRefund(dto);
        return Result.success(true);
    }


    /**
     * 执行 exportTransactions 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    @GetMapping("/export-transactions")
    public Result<List<PayExportItemDTO>> exportTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(payService.exportTransactionVOs(startDate, endDate).stream()
            .map(this::toExportItem)
            .toList());
    }


    /**
     * 转换或解析导出Item。
     * @param vo vo
     * @return 处理结果
     */
    private PayExportItemDTO toExportItem(PayTransactionVO vo) {
        return new PayExportItemDTO(vo.getPaymentNo(), vo.getOrderNo(), vo.getUserId(), vo.getMerchantId(),
            vo.getAmount(), vo.getChannel(), vo.getStatus(), vo.getCreateTime());
    }
}
