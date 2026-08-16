package com.yirancrazy.minimall.api.fallback;

import java.util.List;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayFeign Feign 降级工厂，处理PayFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class PayFeignFallbackFactory implements FallbackFactory<PayFeignClient> {
    @Override
    public PayFeignClient create(Throwable cause) {
        log.warn("pay-service unreachable: {}", cause.getMessage());
        return new PayFeignClient() {
            @Override
            public Result<Long> create(PayCreateDTO dto) {
                return Result.success(-1L);
            }

            @Override
            public Result<Boolean> refund(RefundCreateDTO dto) {
                return Result.success(false);
            }

            @Override
            public Result<List<PayExportItemDTO>> exportTransactions(String startDate, String endDate) {
                log.warn("pay export-transactions fallback, startDate={}, endDate={} skipped", startDate, endDate);
                return Result.success(List.of());
            }
        };
    }
}
