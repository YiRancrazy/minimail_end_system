package com.yirancrazy.minimall.api.fallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public GoodsFeignClient create(Throwable cause) {
        log.warn("goods-service 不可达: {}", cause.getMessage());
        return new GoodsFeignClient() {
            /**
             * 执行 skuSnapshot 方法。
             * @param id id 参数
             * @return 统一响应结果
             */
            @Override
            public Result<SkuSnapshotDTO> skuSnapshot(Long id) {
                return Result.success(null);
            }

            /**
             * 执行 spuSnapshot 方法。
             * @param id id 参数
             * @return 统一响应结果
             */
            @Override
            public Result<SpuSnapshotDTO> spuSnapshot(Long id) {
                return Result.success(null);
            }

            /**
             * 执行 batchSkuSnapshot 方法。
             * @param skuIds skuIds 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Map<Long, SkuSnapshotDTO>> batchSkuSnapshot(List<Long> skuIds) {
                return Result.success(Collections.emptyMap());
            }

            /**
             * 执行 batchSpuSnapshot 方法。
             * @param spuIds spuIds 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Map<Long, SpuSnapshotDTO>> batchSpuSnapshot(List<Long> spuIds) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
