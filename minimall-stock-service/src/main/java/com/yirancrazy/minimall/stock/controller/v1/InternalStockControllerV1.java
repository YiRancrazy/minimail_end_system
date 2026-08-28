package com.yirancrazy.minimall.stock.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存内部控制器，提供Stock相关内部接口
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@RestController
@RequestMapping("/internal/stock")
public class InternalStockControllerV1 {

    private final StockService stockService;

    public InternalStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 按 SKU 预占指定数量库存，库存不存在或不足时抛出业务异常。
     *
     * @param dto 库存预占入参，含 SKU 标识与预占数量
     * @return 统一响应体，数据为预占是否成功
     */
    @PostMapping("/reserve")
    public Result<Boolean> reserve(@Valid @RequestBody StockReserveDTO dto) {
        return Result.success(stockService.reserve(dto.getSkuId(), dto.getQuantity()));
    }

    /**
     * 按 SKU 释放指定数量库存，用于取消订单或超时回滚场景。
     *
     * @param dto 库存释放入参，含 SKU 标识与释放数量
     * @return 统一响应体，数据为释放是否成功
     */
    @PostMapping("/release")
    public Result<Boolean> release(@Valid @RequestBody StockReserveDTO dto) {
        return Result.success(stockService.release(dto.getSkuId(), dto.getQuantity()));
    }

    /**
     * 商品服务创建 SKU 时联动初始化库存记录（幂等）。
     * @param dto SKU 与初始库存入参
     * @return 统一响应体
     */
    @PostMapping("/init")
    public Result<Void> init(@Valid @RequestBody StockInitDTO dto) {
        stockService.initStock(dto.getSkuId(), dto.getMerchantId(), dto.getInitialQuantity());
        return Result.success();
    }
}