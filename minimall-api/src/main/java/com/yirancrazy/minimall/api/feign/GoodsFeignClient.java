package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向订单、购物车和收藏等调用方提供 SKU 与 SPU 快照查询.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-goods-service", fallbackFactory = GoodsFeignFallbackFactory.class)
public interface GoodsFeignClient {
  /**
   * 查询单个 SKU 的下单快照，包括名称、价格、库存和商家归属.
   *
   * @param id SKU ID
   * @return 包装后的 SKU 快照
   */
  @GetMapping("/internal/goods/sku/{id}")
  Result<SkuSnapshotDto> skuSnapshot(@PathVariable("id") Long id);

  /**
   * 查询单个 SPU 的展示快照，包括标题和主图.
   *
   * @param id SPU ID
   * @return 包装后的 SPU 快照
   */
  @GetMapping("/internal/goods/spu/{id}")
  Result<SpuSnapshotDto> spuSnapshot(@PathVariable("id") Long id);

  /**
   * 批量查询 SKU 快照，并以 SKU ID 为键返回.
   *
   * @param skuIds 待查询 SKU ID 列表
   * @return 包装后的 SKU ID 到快照映射
   */
  @PostMapping("/internal/goods/sku/batch-snapshot")
  Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(@RequestBody List<Long> skuIds);

  /**
   * 批量查询 SPU 快照，并以 SPU ID 为键返回.
   *
   * @param spuIds 待查询 SPU ID 列表
   * @return 包装后的 SPU ID 到快照映射
   */
  @PostMapping("/internal/goods/spu/batch-snapshot")
  Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(@RequestBody List<Long> spuIds);
}
