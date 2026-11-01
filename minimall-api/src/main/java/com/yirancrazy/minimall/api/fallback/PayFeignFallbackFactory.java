package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在支付服务不可用时返回无效支付单号、退款失败或空导出结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class PayFeignFallbackFactory implements FallbackFactory<PayFeignClient> {
    /**
     * 为支付远程调用异常创建降级客户端.
     *
     * @param cause 支付远程调用异常
     * @return 支付降级客户端
     */
    @Override
    public PayFeignClient create(Throwable cause) {
        log.warn("pay-service 不可达: {}", cause.getMessage());
        return new PayFeignClient() {
            /**
             * 支付服务不可用时返回 -1 作为无效支付单号标记.
             *
             * @param dto 支付创建参数
             * @return 包含 -1 的成功响应
             */
            @Override
            public Result<Long> create(PayCreateDto dto) {
                return Result.success(-1L);
            }

            /**
             * 支付服务不可用时返回退款未成功.
             *
             * @param dto 退款参数
             * @return 数据为 false 的成功响应
             */
            @Override
            public Result<Boolean> refund(RefundCreateDto dto) {
                return Result.success(false);
            }

            /**
             * 支付服务不可用时返回空支付流水，避免平台导出失败.
             *
             * @param startDate 起始日期
             * @param endDate 结束日期
             * @return 空支付流水列表
             */
            @Override
            public Result<List<PayExportItemDto>> exportTransactions(String startDate, String endDate) {
                log.warn("支付流水导出降级，startDate={}，endDate={}，已跳过", startDate, endDate);
                return Result.success(List.of());
            }
        };
    }
}
