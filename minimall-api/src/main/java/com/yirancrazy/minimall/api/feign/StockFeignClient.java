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
 * @Description: StockFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-stock-service", fallbackFactory = StockFeignFallbackFactory.class)
public interface StockFeignClient {
    /**
     * 执行 reserve 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/stock/reserve")
    Result<Boolean> reserve(@RequestBody StockReserveDTO dto);


    /**
     * 执行 reserveBatch 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/stock/reserve-batch")
    Result<Boolean> reserveBatch(@RequestBody StockBatchReserveDTO dto);


    /**
     * 执行 release 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/stock/release")
    Result<Boolean> release(@RequestBody StockReserveDTO dto);


    /**
     * 初始化相关资源。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/stock/init")
    Result<Void> initStock(@RequestBody StockInitDTO dto);


    /**
     * 执行 adjustStock 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/stock/adjust")
    Result<Void> adjustStock(@RequestBody StockAdjustDTO dto);
}
