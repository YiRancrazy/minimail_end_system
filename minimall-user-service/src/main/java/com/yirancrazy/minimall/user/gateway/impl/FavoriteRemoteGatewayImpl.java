package com.yirancrazy.minimall.user.gateway.impl;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.gateway.FavoriteRemoteGateway;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收藏商品远程查询实现，通过 GoodsFeignClient 批量读取 SKU 和 SPU 快照，Feign fallback 结果由上层容错处理.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class FavoriteRemoteGatewayImpl implements FavoriteRemoteGateway {

    private final GoodsFeignClient goodsFeignClient;

    /**
     * 通过商品服务 Feign 客户端批量读取 SKU 快照。
     *
     * @param skuIds SKU ID 列表
     * @return 商品服务响应；远程异常和降级由 Feign fallback 机制处理
     */
    @Override
    public Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(List<Long> skuIds) {
        return goodsFeignClient.batchSkuSnapshot(skuIds);
    }

    /**
     * 通过商品服务 Feign 客户端批量读取 SPU 快照。
     *
     * @param spuIds SPU ID 列表
     * @return 商品服务响应；远程异常和降级由 Feign fallback 机制处理
     */
    @Override
    public Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(List<Long> spuIds) {
        return goodsFeignClient.batchSpuSnapshot(spuIds);
    }
}
