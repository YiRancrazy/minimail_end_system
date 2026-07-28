package com.yirancrazy.minimall.stock.controller.v1;

import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock")
public class StockControllerV1 {

    private final StockService stockService;

    public StockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{skuId}")
    public Result<Long> get(@PathVariable Long skuId) {
        return Result.success(stockService.query(skuId));
    }
}