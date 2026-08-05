package com.yirancrazy.minimall.stock.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.dto.StockCorrectDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDTO;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端库存控制器，提供平台视角的库存 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@RestController
@RequestMapping("/api/v1/platform/stock")
public class PlatformStockControllerV1 {

    private final StockService stockService;

    public PlatformStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 平台分页查询全平台库存，可选按 SKU 过滤或仅查预警库存。
     * @param dto 分页查询入参
     * @return 库存分页结果
     */
    @GetMapping("/page")
    public Result<CursorPageVO<StockPO>> platformPage(@Valid StockPageDTO dto) {
        return Result.success(stockService.page(dto));
    }

    /**
     * 全平台库存统计聚合，返回SKU总数、可用/预占总量、预警SKU数及预警比例。
     * @return 库存统计VO
     */
    @GetMapping("/statistics")
    public Result<StockStatisticsVO> statistics() {
        return Result.success(stockService.platformStatistics());
    }

    /**
     * 跨商家库存调拨，扣减源SKU库存并增加目标SKU库存。
     * @param operatorId 操作人ID（Header注入）
     * @param dto 调拨入参
     * @return 统一响应体
     */
    @PostMapping("/transfer")
    public Result<Void> transfer(@RequestHeader("X-User-Id") Long operatorId,
                                 @Valid @RequestBody StockTransferDTO dto) {
        dto.setOperatorId(operatorId);
        stockService.transfer(dto);
        return Result.success(null);
    }

    /**
     * 分页查询调拨记录，可选按源/目标SKU过滤。
     * @param dto 分页查询入参
     * @return 调拨记录分页结果
     */
    @GetMapping("/transfers")
    public Result<CursorPageVO<StockTransferPO>> transferPage(@Valid StockTransferPageDTO dto) {
        return Result.success(stockService.transferPage(dto));
    }

    /**
     * 下发库存盘点任务，查询当前可用库存作为期望数量。
     * @param operatorId 操作人ID（Header注入）
     * @param dto 创建入参
     * @return 统一响应体，数据为盘点任务ID
     */
    @PostMapping("/count-tasks")
    public Result<Long> createCountTask(@RequestHeader("X-User-Id") Long operatorId,
                                        @Valid @RequestBody StockCountTaskCreateDTO dto) {
        dto.setOperatorId(operatorId);
        return Result.success(stockService.createCountTask(dto));
    }

    /**
     * 分页查询盘点任务，可选按SKU和状态过滤。
     * @param dto 分页查询入参
     * @return 盘点任务分页结果
     */
    @GetMapping("/count-tasks")
    public Result<CursorPageVO<StockCountTaskPO>> countTaskPage(@Valid StockCountTaskPageDTO dto) {
        return Result.success(stockService.countTaskPage(dto));
    }

    /**
     * 完成盘点任务，计算差异并调整库存。
     * @param id 任务ID
     * @param dto 完成入参
     * @return 统一响应体
     */
    @PostMapping("/count-tasks/{id}/complete")
    public Result<Void> completeCountTask(@PathVariable Long id,
                                          @Valid @RequestBody StockCountTaskCompleteDTO dto) {
        stockService.completeCountTask(id, dto);
        return Result.success(null);
    }

    /**
     * 取消盘点任务。
     * @param id 任务ID
     * @param operatorId 操作人ID（Header注入）
     * @return 统一响应体
     */
    @PostMapping("/count-tasks/{id}/cancel")
    public Result<Void> cancelCountTask(@PathVariable Long id,
                                        @RequestHeader("X-User-Id") Long operatorId) {
        stockService.cancelCountTask(id, operatorId);
        return Result.success(null);
    }

    /**
     * 查询异常库存记录（available &lt; 0 或 reserved &lt; 0）。
     * @return 异常库存列表
     */
    @GetMapping("/abnormal")
    public Result<List<StockPO>> listAbnormal() {
        return Result.success(stockService.listAbnormalStock());
    }

    /**
     * 纠正指定SKU的库存至指定值。
     * @param skuId SKU标识
     * @param operatorId 操作人ID（Header注入）
     * @param dto 纠正入参
     * @return 统一响应体
     */
    @PostMapping("/{skuId}/correct")
    public Result<Void> correct(@PathVariable Long skuId,
                                @RequestHeader("X-User-Id") Long operatorId,
                                @Valid @RequestBody StockCorrectDTO dto) {
        stockService.correctStock(skuId, dto.getCorrectQuantity(), dto.getReason());
        return Result.success(null);
    }
}
