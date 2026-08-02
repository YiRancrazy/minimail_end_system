package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsFeign Feign 降级工厂，处理GoodsFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignClient> {
    @Override
    public GoodsFeignClient create(Throwable cause) {
        log.warn("goods-service unreachable: {}", cause.getMessage());
        return new GoodsFeignClient() {
            @Override
            public Result<SkuSnapshotDTO> skuSnapshot(Long id) {
                return Result.success(null);
            }
        };
    }
}
