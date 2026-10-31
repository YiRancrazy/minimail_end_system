package com.yirancrazy.minimall.pay.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.pay.dto.PayPageDto;
import com.yirancrazy.minimall.pay.dto.PayStatementDto;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVo;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVo;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台侧支付流水、统计、对账单、冻结和导出接口。
 * 查询不受商家请求头限制；服务调用依赖 `PayService`，写操作仅暴露支付单冻结。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/pay")
public class PlatformPayControllerV1 {

  private static final String[] TX_HEADERS = {"支付单号", "订单号", "用户ID", "商家ID", "金额", "状态", "创建时间"};

  private final PayService payService;

  /**
   * 注入平台支付查询、统计和冻结服务。
   *
   * @param payService 平台支付业务服务
   */
  public PlatformPayControllerV1(PayService payService) {
    this.payService = payService;
  }

  /**
   * 分页查询全平台支付流水。
   *
   * @param dto 游标、状态和时间筛选条件
   * @return 平台支付流水游标页
   */
  @GetMapping("/transactions")
  public Result<CursorPageVo<PayTransactionVo>> transactions(@Valid PayPageDto dto) {
    return Result.success(payService.platformPageVo(dto));
  }

  /**
   * 统计全平台支付金额、退款金额和交易笔数。
   *
   * @param dto 统计时间范围筛选条件
   * @return 平台支付统计视图
   */
  @GetMapping("/statistics")
  public Result<PayStatisticsVo> statistics(@Valid PayPageDto dto) {
    return Result.success(payService.statistics(null, dto));
  }

  /**
   * 生成指定日期区间的平台支付对账单。
   * 结束日期包含全天，服务层会转换为次日零点作为排他上界。
   *
   * @param dto 必填的起止日期
   * @return 支付、退款和冻结维度的汇总对账单
   */
  @GetMapping("/statements")
  public Result<PayStatementVo> statement(@Valid PayStatementDto dto) {
    return Result.success(payService.statement(dto));
  }

  /**
   * 将指定支付单冻结，供平台人工处置异常资金。
   *
   * @param paymentNo 支付单号
   * @return 冻结完成后的空成功响应
   */
  @PostMapping("/{paymentNo}/freeze")
  public Result<Void> freeze(@PathVariable("paymentNo") String paymentNo) {
    payService.freeze(paymentNo);
    return Result.success(null);
  }

  /**
   * 按查询条件导出支付流水 CSV。
   * 最多导出公共组件允许的行数，并在响应中写入固定文件名和中文表头。
   *
   * @param dto 状态和时间筛选条件
   * @param response HTTP 响应，用于写入 CSV 内容
   * @throws IOException 输出流写入失败时抛出
   */
  public void exportTransactions(@Valid PayPageDto dto, HttpServletResponse response)
      throws IOException {
    List<PayTransactionVo> list = payService.exportTransactionVos(dto);
    List<String[]> rows = new ArrayList<>(list.size());
    for (PayTransactionVo vo : list) {
      rows.add(
          new String[] {
            vo.getPaymentNo(),
            vo.getOrderNo(),
            String.valueOf(vo.getUserId()),
            String.valueOf(vo.getMerchantId()),
            vo.getAmount() == null ? "" : vo.getAmount().toPlainString(),
            String.valueOf(vo.getStatus()),
            vo.getCreateTime() == null ? "" : vo.getCreateTime().toString()
          });
    }
    CsvExporter.write(response, "transactions.csv", TX_HEADERS, rows);
  }
}
