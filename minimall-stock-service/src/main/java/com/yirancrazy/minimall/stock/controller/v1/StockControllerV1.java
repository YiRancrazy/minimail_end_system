package com.yirancrazy.minimall.stock.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.service.StockService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存控制器，提供Stock RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@RestController
@RequestMapping("/v1/stock")
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

    /**
     * 设置库存预警阈值。
     *
     * @param skuId     SKU 标识
     * @param threshold 预警阈值，必须 >= 0
     * @return 统一响应体
     */
    @PostMapping("/{skuId}/threshold")
    public Result<Void> setThreshold(@PathVariable Long skuId,
                                     @RequestParam Long threshold) {
        stockService.setThreshold(skuId, threshold);
        return Result.success(null);
    }

    /**
     * 手动调整库存数量。
     *
     * @param skuId    SKU 标识
     * @param quantity 调整数量，正数增加、负数扣减，不能为0
     * @param reason   调整原因
     * @return 统一响应体
     */
    @PostMapping("/{skuId}/adjust")
    public Result<Void> adjustStock(@PathVariable Long skuId,
                                    @RequestParam Long quantity,
                                    @RequestParam(required = false) String reason) {
        stockService.adjustStock(skuId, quantity, reason);
        return Result.success(null);
    }

    /**
     * 查询指定SKU的库存流水记录。
     *
     * @param skuId SKU 标识
     * @return 统一响应体，数据为库存流水列表
     */
    @GetMapping("/{skuId}/journal")
    public Result<List<StockJournalPO>> queryJournal(@PathVariable Long skuId) {
        return Result.success(stockService.queryJournal(skuId));
    }
}
