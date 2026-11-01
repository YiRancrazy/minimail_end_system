package com.yirancrazy.minimall.goods.gateway;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装商品服务对商家店铺快照和库存服务的远程调用，隔离 Feign 细节。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface GoodsRemoteGateway {

    /**
     * 查询店铺快照，用于商品发布前校验店铺归属与营业状态。
     *
     * @param shopId 店铺主键，必须属于当前商家且处于营业状态
     * @return 远程服务统一响应；调用方需检查 code 和 data
     */
    Result<ShopSnapshotDto> shopSnapshot(Long shopId);

    /**
     * 请求库存服务为新建 SKU 初始化权威库存。
     *
     * @param dto 请求参数
     * @return 远程服务统一响应；调用方需检查 code 和 data
     */
    Result<Void> initStock(StockInitDto dto);

    /**
     * 请求库存服务按增量调整指定商家 SKU 的库存。
     *
     * @param dto 请求参数
     * @return 远程服务统一响应；调用方需检查 code 和 data
     */
    Result<Void> adjustStock(StockAdjustDto dto);

    /**
     * 批量查询 SKU 权威可用库存，供商品库存缓存同步使用。
     *
     * @param dto 请求参数
     * @return 远程服务统一响应；调用方需检查 code 和 data
     */
    Result<List<StockAvailabilityDto>> queryAvailability(StockAvailabilityQueryDto dto);
}
