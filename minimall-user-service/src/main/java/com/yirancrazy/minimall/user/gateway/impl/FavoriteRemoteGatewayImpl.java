package com.yirancrazy.minimall.user.gateway.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.gateway.FavoriteRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: FavoriteRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class FavoriteRemoteGatewayImpl implements FavoriteRemoteGateway {

    private final GoodsFeignClient goodsFeignClient;

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
}
