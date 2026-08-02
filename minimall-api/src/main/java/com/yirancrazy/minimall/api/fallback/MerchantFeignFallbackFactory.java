package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantFeign Feign 降级工厂，处理MerchantFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class MerchantFeignFallbackFactory implements FallbackFactory<MerchantFeignClient> {
    @Override
    public MerchantFeignClient create(Throwable cause) {
        log.warn("merchant-service unreachable, returning sentinel shop: {}", cause.getMessage());
        return new MerchantFeignClient() {
            @Override
            public Result<ShopSnapshotDTO> shopSnapshot(Long id) {
                return Result.success(new ShopSnapshotDTO(-1L, "unknown", "DOWN"));
            }
        };
    }
}
