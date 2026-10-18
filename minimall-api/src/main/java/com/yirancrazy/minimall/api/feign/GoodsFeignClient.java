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
 * @Description: GoodsFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-goods-service", fallbackFactory = GoodsFeignFallbackFactory.class)
public interface GoodsFeignClient {
    /**
     * 执行 skuSnapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/goods/sku/{id}")
    Result<SkuSnapshotDTO> skuSnapshot(@PathVariable("id") Long id);

    /**
     * 执行 spuSnapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/goods/spu/{id}")
    Result<SpuSnapshotDTO> spuSnapshot(@PathVariable("id") Long id);


    /**
     * 执行 batchSkuSnapshot 方法。
     * @param skuIds skuIds 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/goods/sku/batch-snapshot")
    Result<Map<Long, SkuSnapshotDTO>> batchSkuSnapshot(@RequestBody List<Long> skuIds);


    /**
     * 执行 batchSpuSnapshot 方法。
     * @param spuIds spuIds 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/goods/spu/batch-snapshot")
    Result<Map<Long, SpuSnapshotDTO>> batchSpuSnapshot(@RequestBody List<Long> spuIds);
}
