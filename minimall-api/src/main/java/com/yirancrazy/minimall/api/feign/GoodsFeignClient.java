package com.yirancrazy.minimall.api.feign;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Goods Feign 客户端，调用Goods服务接口
 * @Version: 1.3
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-goods-service", fallbackFactory = GoodsFeignFallbackFactory.class)
public interface GoodsFeignClient {
    @GetMapping("/internal/goods/sku/{id}")
    Result<SkuSnapshotDTO> skuSnapshot(@PathVariable("id") Long id);

    @GetMapping("/internal/goods/spu/{id}")
    Result<SpuSnapshotDTO> spuSnapshot(@PathVariable("id") Long id);

    /**
     * 批量查询 SKU 快照，供购物车列表等场景一次调用替代逐 SKU 的 N 次请求。
     * 仅包含所属 SPU 在售的 SKU，其余不放入返回 Map，由调用方按缺失兜底降级。
     * @param skuIds SKU 主键集合，允许为空
     * @return skuId -> SKU 快照；goods-service 不可用时返回空 Map
     */
    @PostMapping("/internal/goods/sku/batch-snapshot")
    Result<Map<Long, SkuSnapshotDTO>> batchSkuSnapshot(@RequestBody List<Long> skuIds);

    /**
     * 批量查询 SPU 快照，供购物车列表等场景一次调用替代逐 SPU 的 N 次请求。
     * 不存在的 SPU 不放入返回 Map。
     * @param spuIds SPU 主键集合，允许为空
     * @return spuId -> SPU 快照；goods-service 不可用时返回空 Map
     */
    @PostMapping("/internal/goods/spu/batch-snapshot")
    Result<Map<Long, SpuSnapshotDTO>> batchSpuSnapshot(@RequestBody List<Long> spuIds);
}
