package com.yirancrazy.minimall.cart.gateway.impl;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.cart.gateway.CartRemoteGateway;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 GoodsFeignClient 和 UserFeignClient 直接实现购物车远程依赖。该适配器不执行
 *     重试、缓存或响应语义转换，这些决策仍由 CartService 或 Feign 降级配置负责。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class CartRemoteGatewayImpl implements CartRemoteGateway {

    private final GoodsFeignClient goodsFeignClient;
    private final UserFeignClient userFeignClient;

    /**
     * 将批量 SKU 快照查询委托给商品服务.
     *
     * @param skuIds 待解析的 SKU 主键
     * @return 以 SKU 主键映射的商品服务响应
     */
    @Override
    public Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(List<Long> skuIds) {
        return goodsFeignClient.batchSkuSnapshot(skuIds);
    }

    /**
     * 将批量 SPU 快照查询委托给商品服务.
     *
     * @param spuIds 待解析的 SPU 主键
     * @return 以 SPU 主键映射的商品服务响应
     */
    @Override
    public Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(List<Long> spuIds) {
        return goodsFeignClient.batchSpuSnapshot(spuIds);
    }

    /**
     * 将收藏创建操作委托给用户服务.
     *
     * @param userId 收藏记录所属用户
     * @param skuId 待收藏的 SKU
     * @return 表示远程成功或失败的用户服务响应
     */
    @Override
    public Result<Void> addFavorite(Long userId, Long skuId) {
        return userFeignClient.addFavorite(userId, skuId);
    }
}
