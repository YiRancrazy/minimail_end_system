package com.yirancrazy.minimall.stock.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalStockControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/stock")
public class InternalStockControllerV1 {

    private final StockService stockService;

    /**
     * 构造 InternalStockControllerV1 实例。
     * @param stockService stockService 参数
     */
    public InternalStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }


    /**
     * 执行 reserve 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/reserve")
    public Result<Boolean> reserve(@Valid @RequestBody StockReserveDTO dto) {
        return Result.success(stockService.reserve(dto.getSkuId(), dto.getQuantity()));
    }


    /**
     * 执行 reserveBatch 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/reserve-batch")
    public Result<Boolean> reserveBatch(@Valid @RequestBody StockBatchReserveDTO dto) {
        return Result.success(stockService.reserveBatch(dto.getItems()));
    }


    /**
     * 执行 release 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/release")
    public Result<Boolean> release(@Valid @RequestBody StockReserveDTO dto) {
        return Result.success(stockService.release(dto.getSkuId(), dto.getQuantity()));
    }


    /**
     * 初始化相关资源。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/init")
    public Result<Void> init(@Valid @RequestBody StockInitDTO dto) {
        stockService.initStock(dto.getSkuId(), dto.getMerchantId(), dto.getInitialQuantity());
        return Result.success();
    }


    /**
     * 执行 adjust 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/adjust")
    public Result<Void> adjust(@Valid @RequestBody StockAdjustDTO dto) {
        stockService.adjustStock(dto.getSkuId(), dto.getQuantity(), dto.getReason(),
            dto.getMerchantId());
        return Result.success();
    }
}
