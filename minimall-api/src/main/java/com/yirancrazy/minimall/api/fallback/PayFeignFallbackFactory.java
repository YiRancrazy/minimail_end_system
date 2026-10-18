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
 * @Description: PayFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class PayFeignFallbackFactory implements FallbackFactory<PayFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public PayFeignClient create(Throwable cause) {
        log.warn("pay-service 不可达: {}", cause.getMessage());
        return new PayFeignClient() {
            /**
             * 创建业务数据。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Long> create(PayCreateDTO dto) {
                return Result.success(-1L);
            }

            /**
             * 执行 refund 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Boolean> refund(RefundCreateDTO dto) {
                return Result.success(false);
            }

            /**
             * 执行 exportTransactions 方法。
             * @param startDate startDate 参数
             * @param endDate endDate 参数
             * @return 统一响应结果
             */
            @Override
            public Result<List<PayExportItemDTO>> exportTransactions(String startDate, String endDate) {
                log.warn("支付流水导出降级，startDate={}，endDate={}，已跳过", startDate, endDate);
                return Result.success(List.of());
            }
        };
    }
}
