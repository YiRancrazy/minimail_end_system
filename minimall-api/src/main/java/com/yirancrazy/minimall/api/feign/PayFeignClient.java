package com.yirancrazy.minimall.api.feign;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.fallback.PayFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Pay Feign 客户端，调用Pay服务接口
 * @Version: 1.3
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-pay-service", fallbackFactory = PayFeignFallbackFactory.class)
public interface PayFeignClient {
    @PostMapping("/internal/pay/create")
    Result<Long> create(@RequestBody PayCreateDTO dto);

    /**
     * Create a refund for an existing payment.
     * @param dto the refund creation request
     * @return true if the refund was created and processed successfully
     */
    @PostMapping("/internal/pay/refund")
    Result<Boolean> refund(@RequestBody RefundCreateDTO dto);

    /**
     * 全平台支付流水导出列表，按创建时间倒序，服务端限制最大导出行数。
     * @param startDate 起始日期（yyyy-MM-dd），null 表示不限制
     * @param endDate 结束日期（yyyy-MM-dd），null 表示不限制
     * @return 支付流水导出项列表；服务不可用时由 fallback 返回空列表
     */
    @GetMapping("/internal/pay/export-transactions")
    Result<List<PayExportItemDTO>> exportTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);
}
