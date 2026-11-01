package com.yirancrazy.minimall.goods.schedule;

import com.yirancrazy.minimall.goods.service.SkuStockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按配置间隔触发 SKU 库存缓存同步，并吞掉单次调度异常避免任务线程终止。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "minimall.goods.stock-sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SkuStockSyncSchedule {

    private final SkuStockSyncService skuStockSyncService;

    /**
     * 按配置的固定延迟触发库存缓存同步；单次异常仅记录错误日志，避免后续调度被终止。
     */
    @Scheduled(
            fixedDelayString = "${minimall.goods.stock-sync.interval-ms:300000}",
            initialDelayString = "${minimall.goods.stock-sync.initial-delay-ms:60000}")
    public void syncStockCache() {
        try {
            int updated = skuStockSyncService.syncStockCache();
            if (updated > 0) {
                log.info("库存缓存同步完成，更新SKU数={}", updated);
            }
        } catch (Exception e) {
            log.error("库存缓存同步失败", e);
        }
    }
}
