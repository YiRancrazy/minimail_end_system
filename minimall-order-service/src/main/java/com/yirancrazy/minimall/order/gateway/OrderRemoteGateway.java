package com.yirancrazy.minimall.order.gateway;

import java.util.List;
import java.util.Map;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderRemoteGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OrderRemoteGateway {


    /**
     * 执行 skuSnapshot 方法。
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    Result<SkuSnapshotDTO> skuSnapshot(Long skuId);


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


    /**
     * 执行 reserve 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Boolean> reserve(StockReserveDTO dto);


    /**
     * 执行 reserveBatch 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Boolean> reserveBatch(StockBatchReserveDTO dto);


    /**
     * 执行 release 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Boolean> release(StockReserveDTO dto);


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Long> createPayment(PayCreateDTO dto);


    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 统一响应结果
     */
    Result<Long> nextId(String bizTag);


    /**
     * 执行 refund 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Boolean> refund(RefundCreateDTO dto);
}
