package com.yirancrazy.minimall.stock.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.dto.StockCorrectDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDTO;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存控制器，提供Stock RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
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

    /**
     * 平台分页查询全平台库存，可选按 SKU 过滤或仅查预警库存。
     * @param dto 分页查询入参
     * @return 库存分页结果
     */
    @GetMapping("/platform/page")
    public Result<IPage<StockPO>> platformPage(@Valid StockPageDTO dto) {
        return Result.success(stockService.page(dto));
    }

    /**
     * 全平台库存统计聚合，返回SKU总数、可用/预占总量、预警SKU数及预警比例。
     * @return 库存统计VO
     */
    @GetMapping("/platform/statistics")
    public Result<StockStatisticsVO> platformStatistics() {
        return Result.success(stockService.platformStatistics());
    }

    /**
     * 跨商家库存调拨，扣减源SKU库存并增加目标SKU库存。
     * @param operatorId 操作人ID（Header注入）
     * @param dto 调拨入参
     * @return 统一响应体
     */
    @PostMapping("/platform/transfer")
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
    @GetMapping("/platform/transfers")
    public Result<IPage<StockTransferPO>> transferPage(@Valid StockTransferPageDTO dto) {
        return Result.success(stockService.transferPage(dto));
    }

    /**
     * 下发库存盘点任务，查询当前可用库存作为期望数量。
     * @param operatorId 操作人ID（Header注入）
     * @param dto 创建入参
     * @return 统一响应体，数据为盘点任务ID
     */
    @PostMapping("/platform/count-tasks")
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
    @GetMapping("/platform/count-tasks")
    public Result<IPage<StockCountTaskPO>> countTaskPage(@Valid StockCountTaskPageDTO dto) {
        return Result.success(stockService.countTaskPage(dto));
    }

    /**
     * 完成盘点任务，计算差异并调整库存。
     * @param id 任务ID
     * @param dto 完成入参
     * @return 统一响应体
     */
    @PostMapping("/platform/count-tasks/{id}/complete")
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
    @PostMapping("/platform/count-tasks/{id}/cancel")
    public Result<Void> cancelCountTask(@PathVariable Long id,
                                        @RequestHeader("X-User-Id") Long operatorId) {
        stockService.cancelCountTask(id, operatorId);
        return Result.success(null);
    }

    /**
     * 查询异常库存记录（available &lt; 0 或 reserved &lt; 0）。
     * @return 异常库存列表
     */
    @GetMapping("/platform/abnormal")
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
    @PostMapping("/platform/{skuId}/correct")
    public Result<Void> correct(@PathVariable Long skuId,
                                @RequestHeader("X-User-Id") Long operatorId,
                                @Valid @RequestBody StockCorrectDTO dto) {
        stockService.correctStock(skuId, dto.getCorrectQuantity(), dto.getReason());
        return Result.success(null);
    }

    private static final String[] JOURNAL_HEADERS = {
        "流水ID", "SKU", "变动数量", "类型", "原因", "订单号", "时间"
    };

    /**
     * 导出指定SKU的库存流水CSV，最多10000行。
     * @param skuId SKU标识
     * @param response HTTP响应
     * @throws IOException 写入失败时抛出
     */
    @GetMapping("/{skuId}/journal/export")
    public void exportJournal(@PathVariable Long skuId,
                              HttpServletResponse response) throws IOException {
        List<StockJournalPO> list = stockService.exportJournal(skuId);
        CsvExporter.write(response, "stock-journal.csv", JOURNAL_HEADERS, toJournalRows(list));
    }

    private List<String[]> toJournalRows(List<StockJournalPO> list) {
        List<String[]> rows = new ArrayList<>(list.size());
        for (StockJournalPO po : list) {
            rows.add(new String[] {
                String.valueOf(po.getId()),
                String.valueOf(po.getSkuId()),
                String.valueOf(po.getQuantity()),
                journalTypeText(po.getType()),
                po.getReason() == null ? "" : po.getReason(),
                po.getOrderNo() == null ? "" : po.getOrderNo(),
                po.getCreateTime() == null ? "" : po.getCreateTime().toString()
            });
        }
        return rows;
    }

    private static String journalTypeText(Integer type) {
        if (type == null) {
            return "";
        }
        for (StockJournalTypeEnum e : StockJournalTypeEnum.values()) {
            if (e.intCode() == type) {
                return e.getMessage();
            }
        }
        return String.valueOf(type);
    }
}
