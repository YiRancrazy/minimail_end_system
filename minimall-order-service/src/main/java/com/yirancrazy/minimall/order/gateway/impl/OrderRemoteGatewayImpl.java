package com.yirancrazy.minimall.order.gateway.impl;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDTO;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class OrderRemoteGatewayImpl implements OrderRemoteGateway {

    private final GoodsFeignClient goodsFeignClient;
    private final CartFeignClient cartFeignClient;
    private final CartCleanupFeignClient cartCleanupFeignClient;
    private final StockFeignClient stockFeignClient;
    private final PayFeignClient payFeignClient;
    private final IdFeignClient idFeignClient;

    /**
     * 执行 skuSnapshot 方法。
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<SkuSnapshotDTO> skuSnapshot(Long skuId) {
        return goodsFeignClient.skuSnapshot(skuId);
    }

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
     * 查询购物车结算快照。
     * @param dto 查询参数
     * @return 购物车条目快照
     */
    @Override
    public Result<List<CartCheckoutItemDTO>> cartCheckoutSnapshot(CartCheckoutQueryDTO dto) {
        return cartFeignClient.getCheckoutItems(dto);
    }


    /**
     * 清理已结算购物车条目。
     * @param dto 清理参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> cleanupCheckoutCart(CartCheckoutCleanupDTO dto) {
        return cartCleanupFeignClient.cleanupCheckout(dto);
    }

    /**
     * 执行 reserve 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Boolean> reserve(StockReserveDTO dto) {
        return stockFeignClient.reserve(dto);
    }

    /**
     * 执行 reserveBatch 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Boolean> reserveBatch(StockBatchReserveDTO dto) {
        return stockFeignClient.reserveBatch(dto);
    }

    /**
     * 执行 release 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Boolean> release(StockReserveDTO dto) {
        return stockFeignClient.release(dto);
    }

    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Long> createPayment(PayCreateDTO dto) {
        return payFeignClient.create(dto);
    }

    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Long> nextId(String bizTag) {
        return idFeignClient.nextId(bizTag);
    }

    /**
     * 执行 refund 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Boolean> refund(RefundCreateDTO dto) {
        return payFeignClient.refund(dto);
    }
}
