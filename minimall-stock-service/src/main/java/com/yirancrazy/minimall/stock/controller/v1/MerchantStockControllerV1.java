package com.yirancrazy.minimall.stock.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.stock.constant.StockJournalTypeEnum;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockJournalVo;
import jakarta.servlet.http.HttpServletResponse;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供商户侧库存查询、阈值、调整、流水查询和 CSV 导出接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/stock")
public class MerchantStockControllerV1 {

  private static final String[] JOURNAL_HEADERS = {"流水ID", "SKU", "变动数量", "类型", "原因", "订单号", "时间"};

  private final StockService stockService;

  /**
   * 注入库存领域服务。
   *
   * @param stockService 库存业务服务
   */
  public MerchantStockControllerV1(StockService stockService) {
    this.stockService = stockService;
  }

  /**
   * 查询指定商户名下 SKU 的可用库存。
   *
   * @param merchantId 可信请求头中的商户 ID
   * @param skuId SKU ID
   * @return 可用量统一响应；记录不存在时服务返回 0
   */
  @GetMapping("/{skuId}")
  public Result<Long> get(
      @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable("skuId") Long skuId) {
    return Result.success(stockService.query(skuId, merchantId));
  }

  /**
   * 设置指定商户 SKU 的低库存告警阈值。
   *
   * @param merchantId 可信请求头中的商户 ID
   * @param skuId SKU ID
   * @param threshold 非负告警阈值
   * @return 无业务数据的成功响应
   */
  @PostMapping("/{skuId}/threshold")
  public Result<Void> setThreshold(
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @PathVariable Long skuId,
      @RequestParam Long threshold) {
    stockService.setThreshold(skuId, threshold, merchantId);
    return Result.success(null);
  }

  /**
   * 按增量调整商户 SKU 可用库存，并保留调整原因。
   *
   * @param merchantId 可信请求头中的商户 ID
   * @param skuId SKU ID
   * @param quantity 非 0 调整增量
   * @param reason 可选调整原因
   * @return 无业务数据的成功响应
   */
  @PostMapping("/{skuId}/adjust")
  public Result<Void> adjustStock(
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @PathVariable Long skuId,
      @RequestParam Long quantity,
      @RequestParam(required = false) String reason) {
    stockService.adjustStock(skuId, quantity, reason, merchantId);
    return Result.success(null);
  }

  /**
   * 查询指定商户 SKU 最近 200 条库存流水。
   *
   * @param merchantId 可信请求头中的商户 ID
   * @param skuId SKU ID
   * @return 库存流水视图列表统一响应
   */
  @GetMapping("/{skuId}/journal")
  public Result<List<StockJournalVo>> queryJournal(
      @RequestHeader("X-Merchant-Id") Long merchantId, @PathVariable Long skuId) {
    return Result.success(stockService.queryJournalVo(skuId, merchantId));
  }

  /**
   * 将商户可见的库存流水导出为 UTF-8 CSV 响应。
   *
   * @param merchantId 可信请求头中的商户 ID
   * @param skuId SKU ID
   * @param response HTTP 响应，用于写入下载文件名和 CSV 内容
   * @throws IOException 写入响应流失败时抛出
   */
  @GetMapping("/{skuId}/journal/export")
  public void exportJournal(
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @PathVariable Long skuId,
      HttpServletResponse response)
      throws IOException {
    List<StockJournalVo> list = stockService.exportJournalVo(skuId, merchantId);
    CsvExporter.write(response, "stock-journal.csv", JOURNAL_HEADERS, toJournalRows(list));
  }

  /**
   * 将库存流水视图按固定列顺序转换为 CSV 字符串行。
   *
   * @param list 待导出流水
   * @return 与流水一一对应的 CSV 行数组
   */
  private List<String[]> toJournalRows(List<StockJournalVo> list) {
    List<String[]> rows = new ArrayList<>(list.size());
    for (StockJournalVo vo : list) {
      rows.add(
          new String[] {
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
   * 将库存流水类型编码转换为中文显示文本。
   *
   * @param type 流水类型编码
   * @return 已识别类型的中文名称；null 返回空串，未知编码返回原值文本
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
