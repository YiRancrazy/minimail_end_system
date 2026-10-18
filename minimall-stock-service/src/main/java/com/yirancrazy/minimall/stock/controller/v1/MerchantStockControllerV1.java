package com.yirancrazy.minimall.stock.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockJournalVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantStockControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/stock")
public class MerchantStockControllerV1 {

    private static final String[] JOURNAL_HEADERS = {
        "流水ID", "SKU", "变动数量", "类型", "原因", "订单号", "时间"
    };

    private final StockService stockService;

    /**
     * 构造 MerchantStockControllerV1 实例。
     * @param stockService stockService 参数
     */
    public MerchantStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{skuId}")
    public Result<Long> get(@RequestHeader("X-Merchant-Id") Long merchantId,
                            @PathVariable("skuId") Long skuId) {
        return Result.success(stockService.query(skuId, merchantId));
    }


    /**
     * 设置相关数据。
     * @param merchantId merchantId 参数
     * @param skuId skuId 参数
     * @param threshold threshold 参数
     * @return 统一响应结果
     */
    @PostMapping("/{skuId}/threshold")
    public Result<Void> setThreshold(@RequestHeader("X-Merchant-Id") Long merchantId,
                                     @PathVariable Long skuId,
                                     @RequestParam Long threshold) {
        stockService.setThreshold(skuId, threshold, merchantId);
        return Result.success(null);
    }


    /**
     * 执行 adjustStock 方法。
     * @param merchantId merchantId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param reason reason 参数
     * @return 统一响应结果
     */
    @PostMapping("/{skuId}/adjust")
    public Result<Void> adjustStock(@RequestHeader("X-Merchant-Id") Long merchantId,
                                    @PathVariable Long skuId,
                                    @RequestParam Long quantity,
                                    @RequestParam(required = false) String reason) {
        stockService.adjustStock(skuId, quantity, reason, merchantId);
        return Result.success(null);
    }


    /**
     * 执行 queryJournal 方法。
     * @param merchantId merchantId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{skuId}/journal")
    public Result<List<StockJournalVO>> queryJournal(
        @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable Long skuId) {
        return Result.success(stockService.queryJournalVO(skuId, merchantId));
    }


    /**
     * 执行 exportJournal 方法。
     * @param merchantId merchantId 参数
     * @param skuId skuId 参数
     * @param response response 参数
     * @throws IOException 处理失败时抛出
     */
    public void exportJournal(@RequestHeader("X-Merchant-Id") Long merchantId,
                              @PathVariable Long skuId,
                              HttpServletResponse response) throws IOException {
        List<StockJournalVO> list = stockService.exportJournalVO(skuId, merchantId);
        CsvExporter.write(response, "stock-journal.csv", JOURNAL_HEADERS, toJournalRows(list));
    }

    /**
     * 转换或解析JournalRows。
     * @param list 列表
     * @return 结果列表
     */
    private List<String[]> toJournalRows(List<StockJournalVO> list) {
        List<String[]> rows = new ArrayList<>(list.size());
        for (StockJournalVO vo : list) {
            rows.add(new String[] {
                String.valueOf(vo.getId()),
                String.valueOf(vo.getSkuId()),
                String.valueOf(vo.getQuantity()),
                journalTypeText(vo.getType()),
                vo.getReason() == null ? "" : vo.getReason(),
                vo.getOrderNo() == null ? "" : vo.getOrderNo(),
                vo.getCreateTime() == null ? "" : vo.getCreateTime().toString()
            });
        }
        return rows;
    }

    /**
     * 处理journalTypeText。
     * @param type 类型
     * @return 处理结果
     */
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
