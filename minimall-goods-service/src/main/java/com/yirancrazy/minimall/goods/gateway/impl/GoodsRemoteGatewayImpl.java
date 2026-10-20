package com.yirancrazy.minimall.goods.gateway.impl;

import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class GoodsRemoteGatewayImpl implements GoodsRemoteGateway {

    private final MerchantFeignClient merchantFeignClient;
    private final StockFeignClient stockFeignClient;

    /**
     * 执行 shopSnapshot 方法。
     * @param shopId shopId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<ShopSnapshotDTO> shopSnapshot(Long shopId) {
        return merchantFeignClient.shopSnapshot(shopId);
    }

    /**
     * 初始化相关资源。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> initStock(StockInitDTO dto) {
        return stockFeignClient.initStock(dto);
    }

    /**
     * 执行 adjustStock 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> adjustStock(StockAdjustDTO dto) {
        return stockFeignClient.adjustStock(dto);
    }

    /**
     * 批量查询SKU可用库存。
     * @param dto 查询参数
     * @return 库存快照列表
     */
    @Override
    public Result<List<StockAvailabilityDTO>> queryAvailability(StockAvailabilityQueryDTO dto) {
        return stockFeignClient.queryAvailability(dto);
    }
}
