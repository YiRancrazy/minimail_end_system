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
 * @Description: PayFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-pay-service", fallbackFactory = PayFeignFallbackFactory.class)
public interface PayFeignClient {
    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/pay/create")
    Result<Long> create(@RequestBody PayCreateDTO dto);


    /**
     * 执行 refund 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/pay/refund")
    Result<Boolean> refund(@RequestBody RefundCreateDTO dto);


    /**
     * 执行 exportTransactions 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/pay/export-transactions")
    Result<List<PayExportItemDTO>> exportTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate);
}
