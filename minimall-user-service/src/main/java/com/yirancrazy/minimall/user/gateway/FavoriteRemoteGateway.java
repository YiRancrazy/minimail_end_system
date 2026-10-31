package com.yirancrazy.minimall.user.gateway;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import java.util.Map;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收藏展示所需的商品远程查询边界，隔离业务服务与 minimall-api Feign 客户端，并允许 fallback 降级.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface FavoriteRemoteGateway {

  /**
   * 批量查询收藏展示所需的 SKU 快照。
   *
   * @param skuIds SKU ID 列表；调用方应避免传入重复值
   * @return 以 SKU ID 为键的远程查询结果，商品服务不可用时由 Feign fallback 提供降级结果
   */
  Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(List<Long> skuIds);

  /**
   * 批量查询收藏展示所需的 SPU 快照。
   *
   * @param spuIds SPU ID 列表；调用方应避免传入重复值
   * @return 以 SPU ID 为键的远程查询结果，商品服务不可用时由 Feign fallback 提供降级结果
   */
  Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(List<Long> spuIds);
}
