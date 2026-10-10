package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.fallback.StockFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Stock Feign 客户端，调用Stock服务接口
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-stock-service", fallbackFactory = StockFeignFallbackFactory.class)
public interface StockFeignClient {
    @PostMapping("/internal/stock/reserve")
    Result<Boolean> reserve(@RequestBody StockReserveDTO dto);

    /**
     * 批量预占库存，stock-service 在同一事务内处理全部 SKU。
     * @param dto 批量预占请求
     * @return true 表示整批预占成功
     */
    @PostMapping("/internal/stock/reserve-batch")
    Result<Boolean> reserveBatch(@RequestBody StockBatchReserveDTO dto);

    /**
     * 释放 SKU 之前锁定的库存。
     * @param dto 释放请求，复用预留 DTO 结构
     * @return 释放成功时返回 true
     */
    @PostMapping("/internal/stock/release")
    Result<Boolean> release(@RequestBody StockReserveDTO dto);

    /**
     * 商品服务创建 SKU 时联动初始化库存记录（幂等：记录已存在则不覆盖）。
     * @param dto SKU 与初始库存信息
     * @return 统一响应体
     */
    @PostMapping("/internal/stock/init")
    Result<Void> initStock(@RequestBody StockInitDTO dto);

    /**
     * 调整已有 SKU 的权威可用库存，商品服务只同步本地展示投影。
     * @param dto 库存增量与商家归属信息
     * @return 统一响应体
     */
    @PostMapping("/internal/stock/adjust")
    Result<Void> adjustStock(@RequestBody StockAdjustDTO dto);
}
