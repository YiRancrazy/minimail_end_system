package com.yirancrazy.minimall.goods.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityQueryDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.SkuStockSyncService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SKU库存缓存同步服务实现。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
@Slf4j
@Service
public class SkuStockSyncServiceImpl implements SkuStockSyncService {

    private static final int MAX_BATCH_SIZE = 500;
    private static final int MAX_MISSING_SAMPLES = 10;

    private final SkuManager skuManager;
    private final GoodsRemoteGateway goodsRemoteGateway;
    private final int batchSize;

    /**
     * 构造库存缓存同步服务。
     * @param skuManager SKU管理器
     * @param goodsRemoteGateway 远程服务网关
     * @param batchSize 单批同步数量
     */
    public SkuStockSyncServiceImpl(
            SkuManager skuManager,
            GoodsRemoteGateway goodsRemoteGateway,
            @Value("${minimall.goods.stock-sync.batch-size:200}") int batchSize) {
        this.skuManager = skuManager;
        this.goodsRemoteGateway = goodsRemoteGateway;
        this.batchSize = Math.max(1, Math.min(batchSize, MAX_BATCH_SIZE));
    }

    /**
     * 将权威库存同步到商品库存缓存。
     * @return 更新的SKU数量
     */
    @Override
    public int syncStockCache() {
        int updated = 0;
        int missing = 0;
        List<Long> missingSamples = new ArrayList<>(MAX_MISSING_SAMPLES);
        long lastId = 0L;
        while (true) {
            List<SkuPO> skuBatch = skuManager.list(Wrappers.lambdaQuery(SkuPO.class)
                .gt(SkuPO::getId, lastId)
                .orderByAsc(SkuPO::getId)
                .last("LIMIT " + batchSize));
            if (skuBatch.isEmpty()) {
                break;
            }
            lastId = skuBatch.get(skuBatch.size() - 1).getId();
            List<Long> skuIds = skuBatch.stream().map(SkuPO::getId).toList();
            Result<List<StockAvailabilityDTO>> response = goodsRemoteGateway.queryAvailability(
                new StockAvailabilityQueryDTO(skuIds));
            if (response == null || !CommonCode.SUCCESS.equals(response.getCode())) {
                String message = response == null ? "库存服务无响应" : response.getMessage();
                throw new BizException(CommonCode.SYS_ERROR, message);
            }
            Map<Long, Long> availableBySku = new LinkedHashMap<>(skuBatch.size() * 2);
            if (response.getData() != null) {
                for (StockAvailabilityDTO stock : response.getData()) {
                    availableBySku.put(stock.getSkuId(), stock.getAvailable());
                }
            }
            List<SkuPO> changed = new ArrayList<>(skuBatch.size());
            for (SkuPO sku : skuBatch) {
                Long available = availableBySku.get(sku.getId());
                if (available == null) {
                    missing++;
                    if (missingSamples.size() < MAX_MISSING_SAMPLES) {
                        missingSamples.add(sku.getId());
                    }
                    available = 0L;
                }
                int cacheStock = toCacheStock(available);
                if (!Objects.equals(sku.getStock(), cacheStock)) {
                    sku.setStock(cacheStock);
                    changed.add(sku);
                }
            }
            if (!changed.isEmpty() && !skuManager.updateBatchById(changed)) {
                throw new BizException(CommonCode.SYS_ERROR, "库存缓存批量更新失败");
            }
            updated += changed.size();
            if (skuBatch.size() < batchSize) {
                break;
            }
        }
        if (missing > 0) {
            log.warn("库存缓存同步发现缺少权威库存，count={}，skuIds={}", missing, missingSamples);
        }
        return updated;
    }

    private int toCacheStock(long available) {
        if (available <= 0) {
            return 0;
        }
        if (available > Integer.MAX_VALUE) {
            log.warn("权威库存超过商品缓存范围，将缓存值限制为Integer.MAX_VALUE，available={}", available);
            return Integer.MAX_VALUE;
        }
        return (int) available;
    }
}
