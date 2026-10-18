package com.yirancrazy.minimall.cart.gateway.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.cart.gateway.CartRemoteGateway;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class CartRemoteGatewayImpl implements CartRemoteGateway {

    private final GoodsFeignClient goodsFeignClient;
    private final UserFeignClient userFeignClient;

    /**
     * 执行 batchSkuSnapshot 方法。
     * @param skuIds skuIds 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Map<Long, SkuSnapshotDTO>> batchSkuSnapshot(List<Long> skuIds) {
        return goodsFeignClient.batchSkuSnapshot(skuIds);
    }

    /**
     * 执行 batchSpuSnapshot 方法。
     * @param spuIds spuIds 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Map<Long, SpuSnapshotDTO>> batchSpuSnapshot(List<Long> spuIds) {
        return goodsFeignClient.batchSpuSnapshot(spuIds);
    }

    /**
     * 执行 addFavorite 方法。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> addFavorite(Long userId, Long skuId) {
        return userFeignClient.addFavorite(userId, skuId);
    }
}
