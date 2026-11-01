package com.yirancrazy.minimall.stock.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.dto.StockCorrectDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDto;
import com.yirancrazy.minimall.stock.dto.StockPageDto;
import com.yirancrazy.minimall.stock.dto.StockTransferDto;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDto;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockCountTaskVo;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVo;
import com.yirancrazy.minimall.stock.vo.StockTransferVo;
import com.yirancrazy.minimall.stock.vo.StockVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台库存分页、统计、调拨、盘点、异常查询和人工纠正接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/stock")
public class PlatformStockControllerV1 {

    private final StockService stockService;

    /**
     * 注入库存领域服务。
     *
     * @param stockService 库存业务服务
     */
    public PlatformStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 使用游标分页查询平台库存，可按 SKU 和低库存条件筛选。
     *
     * @param dto 分页及筛选条件
     * @return 库存视图游标分页统一响应
     */
    @GetMapping("/page")
    public Result<CursorPageVo<StockVo>> platformPage(@Valid StockPageDto dto) {
        return Result.success(stockService.pageVo(dto));
    }

    /**
     * 查询平台库存总量、告警数量及告警占比。
     *
     * @return 库存统计视图统一响应
     */
    @GetMapping("/statistics")
    public Result<StockStatisticsVo> statistics() {
        return Result.success(stockService.platformStatistics());
    }

    /**
     * 以可信请求头中的用户作为操作人，在源、目标 SKU 之间调拨库存。
     *
     * @param operatorId 可信请求头中的操作人 ID
     * @param dto 调拨明细；控制器会覆盖其中的操作人为请求头值
     * @return 无业务数据的成功响应
     */
    @PostMapping("/transfer")
    public Result<Void> transfer(
            @RequestHeader("X-User-Id") Long operatorId, @Valid @RequestBody StockTransferDto dto) {
        dto.setOperatorId(operatorId);
        stockService.transfer(dto);
        return Result.success(null);
    }

    /**
     * 使用游标分页查询库存调拨审计记录。
     *
     * @param dto 分页及源、目标 SKU 筛选条件
     * @return 调拨视图游标分页统一响应
     */
    @GetMapping("/transfers")
    public Result<CursorPageVo<StockTransferVo>> transferPage(@Valid StockTransferPageDto dto) {
        return Result.success(stockService.transferPageVo(dto));
    }

    /**
     * 以当前 SKU 可用量为账面量创建待盘点任务。
     *
     * @param operatorId 可信请求头中的操作人 ID
     * @param dto 盘点 SKU 和备注；控制器会覆盖其中的操作人
     * @return 新建盘点任务 ID 的统一响应
     */
    @PostMapping("/count-tasks")
    public Result<Long> createCountTask(
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody StockCountTaskCreateDto dto) {
        dto.setOperatorId(operatorId);
        return Result.success(stockService.createCountTask(dto));
    }

    /**
     * 使用游标分页查询盘点任务，可按 SKU 和状态筛选。
     *
     * @param dto 分页及筛选条件
     * @return 盘点任务视图游标分页统一响应
     */
    @GetMapping("/count-tasks")
    public Result<CursorPageVo<StockCountTaskVo>> countTaskPage(@Valid StockCountTaskPageDto dto) {
        return Result.success(stockService.countTaskPageVo(dto));
    }

    /**
     * 提交实盘数量以完成待盘点任务，并触发可能的库存差异调整。
     *
     * @param id 盘点任务 ID
     * @param dto 实盘数量和备注
     * @return 无业务数据的成功响应
     */
    @PostMapping("/count-tasks/{id}/complete")
    public Result<Void> completeCountTask(
            @PathVariable Long id, @Valid @RequestBody StockCountTaskCompleteDto dto) {
        stockService.completeCountTask(id, dto);
        return Result.success(null);
    }

    /**
     * 取消待盘点任务，并记录可信请求头中的操作人。
     *
     * @param id 盘点任务 ID
     * @param operatorId 可信请求头中的操作人 ID
     * @return 无业务数据的成功响应
     */
    @PostMapping("/count-tasks/{id}/cancel")
    public Result<Void> cancelCountTask(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long operatorId) {
        stockService.cancelCountTask(id, operatorId);
        return Result.success(null);
    }

    /**
     * 查询最多 200 条可用量或预占量为负的异常库存。
     *
     * @return 异常库存视图列表统一响应
     */
    @GetMapping("/abnormal")
    public Result<List<StockVo>> listAbnormal() {
        return Result.success(stockService.listAbnormalStockVo());
    }

    /**
     * 将指定 SKU 可用量直接纠正为目标值，并由服务层记录差值流水。
     *
     * @param skuId SKU ID
     * @param operatorId 可信请求头中的操作人 ID；当前实现未传给库存服务
     * @param dto 目标可用量和纠正原因
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{skuId}/correct")
    public Result<Void> correct(
            @PathVariable Long skuId,
            @RequestHeader("X-User-Id") Long operatorId,
            @Valid @RequestBody StockCorrectDto dto) {
        stockService.correctStock(skuId, dto.getCorrectQuantity(), dto.getReason());
        return Result.success(null);
    }
}
