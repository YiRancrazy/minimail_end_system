package com.yirancrazy.minimall.stock.controller.v1;

import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stock")
public class InternalStockControllerV1 {

    private final StockService stockService;

    public InternalStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/reserve")
    public Result<Boolean> reserve(@RequestBody StockReserveDTO dto) {
        return Result.success(stockService.reserve(dto.getSkuId(), dto.getQuantity()));
    }

    @PostMapping("/release")
    public Result<Boolean> release(@RequestBody StockReserveDTO dto) {
        return Result.success(stockService.release(dto.getSkuId(), dto.getQuantity()));
    }
}