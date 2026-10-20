package com.yirancrazy.minimall.goods.gateway;

import java.util.List;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsRemoteGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface GoodsRemoteGateway {


    /**
     * 执行 shopSnapshot 方法。
     * @param shopId shopId 参数
     * @return 统一响应结果
     */
    Result<ShopSnapshotDTO> shopSnapshot(Long shopId);


    /**
     * 初始化相关资源。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Void> initStock(StockInitDTO dto);


    /**
     * 执行 adjustStock 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Void> adjustStock(StockAdjustDTO dto);


    /**
     * 批量查询SKU可用库存。
     * @param dto 查询参数
     * @return 库存快照列表
     */
    Result<List<StockAvailabilityDTO>> queryAvailability(StockAvailabilityQueryDTO dto);
}
