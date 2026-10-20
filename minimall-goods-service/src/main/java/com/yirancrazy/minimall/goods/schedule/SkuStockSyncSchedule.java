package com.yirancrazy.minimall.goods.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.goods.service.SkuStockSyncService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SKU库存缓存定时同步任务。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "minimall.goods.stock-sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SkuStockSyncSchedule {

    private final SkuStockSyncService skuStockSyncService;

    /**
     * 执行库存缓存同步。
     */
    @Scheduled(
        fixedDelayString = "${minimall.goods.stock-sync.interval-ms:300000}",
        initialDelayString = "${minimall.goods.stock-sync.initial-delay-ms:60000}"
    )
    public void syncStockCache() {
        try {
            int updated = skuStockSyncService.syncStockCache();
            if (updated > 0) {
                log.info("库存缓存同步完成，更新SKU数={}", updated);
            }
        }
        catch (Exception e) {
            log.error("库存缓存同步失败", e);
        }
    }
}
