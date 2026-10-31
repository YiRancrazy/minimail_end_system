package com.yirancrazy.minimall.cart.gateway;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import java.util.Map;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 抽象购物车服务对商品和用户远程能力的访问。网关原样返回共享 Result，使
 *     CartService 能决定快照缺失或收藏写入失败时的业务行为，同时把 Feign 细节隔离在服务层之外。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CartRemoteGateway {

  /**
   * 通过一次远程请求加载购物车展示所需的 SKU 快照.
   *
   * @param skuIds SKU 主键列表，调用方通常会预先去重
   * @return 以 SKU 主键映射的商品服务响应
   */
  Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(List<Long> skuIds);

  /**
   * 加载商品标题和主图所需的 SPU 快照.
   *
   * @param spuIds 从 SKU 快照中提取并去重后的 SPU 主键
   * @return 以 SPU 主键映射的商品服务响应
   */
  Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(List<Long> spuIds);

  /**
   * 通过用户服务将一个 SKU 添加到用户收藏夹.
   *
   * @param userId 收藏记录所属用户
   * @param skuId 待收藏的 SKU
   * @return 表示远程写入是否成功的用户服务响应
   */
  Result<Void> addFavorite(Long userId, Long skuId);
}
