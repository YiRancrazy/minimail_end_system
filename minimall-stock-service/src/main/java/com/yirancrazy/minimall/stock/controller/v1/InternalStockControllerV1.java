package com.yirancrazy.minimall.stock.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;

/**
* 库存内部接口控制器，挂载于 /internal/stock，仅供订单等内部服务经 Feign 调用，
 *               提供下单时的库存预占与取消/超时后的库存释放能力，不对外网暴露。
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

    @PostMapping("/release")
    public Result<Boolean> release(@Valid @RequestBody StockReserveDTO dto) {
        return Result.success(stockService.release(dto.getSkuId(), dto.getQuantity()));
    }
}