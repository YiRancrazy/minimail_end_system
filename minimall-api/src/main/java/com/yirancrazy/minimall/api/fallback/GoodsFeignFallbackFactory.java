package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在商品服务不可用时返回空快照，避免非关键展示查询阻塞主流程.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class GoodsFeignFallbackFactory implements FallbackFactory<GoodsFeignClient> {
    /**
     * 为商品远程调用异常创建降级客户端.
     *
     * @param cause 商品远程调用异常
     * @return 商品降级客户端
     */
    @Override
    public GoodsFeignClient create(Throwable cause) {
        log.warn("goods-service 不可达: {}", cause.getMessage());
        return new GoodsFeignClient() {
            /**
             * 商品服务不可用时返回空 SKU 快照.
             *
             * @param id SKU ID
             * @return 数据为空的成功响应
             */
            @Override
            public Result<SkuSnapshotDto> skuSnapshot(Long id) {
                return Result.success(null);
            }

            /**
             * 商品服务不可用时返回空 SPU 快照.
             *
             * @param id SPU ID
             * @return 数据为空的成功响应
             */
            @Override
            public Result<SpuSnapshotDto> spuSnapshot(Long id) {
                return Result.success(null);
            }

            /**
             * 商品服务不可用时返回空 SKU 快照映射.
             *
             * @param skuIds 待查询 SKU ID 列表
             * @return 空映射成功响应
             */
            @Override
            public Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(List<Long> skuIds) {
                return Result.success(Collections.emptyMap());
            }

            /**
             * 商品服务不可用时返回空 SPU 快照映射.
             *
             * @param spuIds 待查询 SPU ID 列表
             * @return 空映射成功响应
             */
            @Override
            public Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(List<Long> spuIds) {
                return Result.success(Collections.emptyMap());
            }
        };
    }
}
