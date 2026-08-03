package com.yirancrazy.minimall.stock.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

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
