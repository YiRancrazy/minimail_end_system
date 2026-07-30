package com.yirancrazy.minimall.stock.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;

/**
* 库存 C 端接口控制器，挂载于 /api/v1/stock，对外提供按 SKU 查询当前
 *               可用库存数量的能力，结果统一包装为 Result 返回。
 */
@RestController
@RequestMapping("/api/v1/stock")
public class StockControllerV1 {

    private final StockService stockService;

    public StockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 按 SKU 标识查询当前可用库存数量，库存记录不存在时返回 0。
     *
     * @param skuId SKU 标识
     * @return 统一响应体，数据为该 SKU 的可用库存数量
     */
    @GetMapping("/{skuId}")
    public Result<Long> get(@PathVariable("skuId") Long skuId) {
        return Result.success(stockService.query(skuId));
    }
}