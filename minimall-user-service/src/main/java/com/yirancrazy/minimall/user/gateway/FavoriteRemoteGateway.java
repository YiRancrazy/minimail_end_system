package com.yirancrazy.minimall.user.gateway;

import java.util.List;
import java.util.Map;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: FavoriteRemoteGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface FavoriteRemoteGateway {


    /**
     * 执行 batchSkuSnapshot 方法。
     * @param skuIds skuIds 参数
     * @return 统一响应结果
     */
    Result<Map<Long, SkuSnapshotDTO>> batchSkuSnapshot(List<Long> skuIds);


    /**
     * 执行 batchSpuSnapshot 方法。
     * @param spuIds spuIds 参数
     * @return 统一响应结果
     */
    Result<Map<Long, SpuSnapshotDTO>> batchSpuSnapshot(List<Long> spuIds);
}
