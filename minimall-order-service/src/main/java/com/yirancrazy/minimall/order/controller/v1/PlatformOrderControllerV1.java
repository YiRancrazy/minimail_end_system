package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVo;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVo;
import com.yirancrazy.minimall.order.vo.OrderSummaryVo;
import com.yirancrazy.minimall.order.vo.OrderTrendVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台端订单分页、状态、详情、关闭、退款仲裁、导出和统计接口。
 *   业务边界：仅负责 HTTP 边界和 CSV 输出，跨商家查询及仲裁规则由 OrderService 承担。
 *   使用场景：平台运营后台通过 /api/v1/platform/orders 调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/orders")
public class PlatformOrderControllerV1 {

  private static final String[] ORDER_HEADERS = {
    "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
  };

  private final OrderService orderService;

  /**
   * 创建平台订单控制器并注入订单服务。
   *
   * <p>平台查询和仲裁均由 OrderService 执行。
   *
   * @param orderService 订单业务服务
   */
  public PlatformOrderControllerV1(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * 查询平台订单游标页。
   *
   * <p>支持用户、商家、状态和时间范围筛选。
   *
   * @param dto 订单分页和筛选参数
   * @return 订单视图游标页统一响应。
   */
  @GetMapping
  public Result<CursorPageVo<OrderVo>> page(@Valid OrderPageDto dto) {
    return Result.success(orderService.pageVo(dto));
  }

  /**
   * 查询平台订单状态码。
   *
   * <p>平台入口按订单 ID 直接读取状态。
   *
   * @param id 订单 ID
   * @return 状态码统一响应。
   */
  @GetMapping("/{id}")
  public Result<Integer> status(@PathVariable("id") Long id) {
    return Result.success(orderService.getStatus(id));
  }

  /**
   * 查询平台订单详情。
   *
   * <p>返回订单视图，不要求用户或商家归属过滤。
   *
   * @param id 订单 ID
   * @return 订单详情统一响应。
   */
  @GetMapping("/{id}/detail")
  public Result<OrderVo> detail(@PathVariable("id") Long id) {
    return Result.success(orderService.platformDetailVo(id));
  }

  /**
   * 由平台关闭订单。
   *
   * <p>关闭和库存释放规则由订单服务执行。
   *
   * @param orderId 订单 ID
   * @return 空数据的成功响应。
   */
  @PostMapping("/{orderId}/close")
  public Result<Void> close(@PathVariable Long orderId) {
    orderService.platformClose(orderId);
    return Result.success(null);
  }

  /**
   * 提交平台退款仲裁。
   *
   * <p>approved=true 请求支付退款，false 驳回并回退状态。
   *
   * @param orderId 订单 ID
   * @param approved true 表示通过，false 表示驳回
   * @return 空数据的成功响应。
   */
  @PostMapping("/{orderId}/arbitrate")
  public Result<Void> refundArbitrate(@PathVariable Long orderId, @RequestParam boolean approved) {
    orderService.arbitrateRefund(orderId, approved);
    return Result.success(null);
  }

  /**
   * 导出平台订单 CSV。
   *
   * <p>按筛选条件查询并写出平台订单文件。
   *
   * @param dto 订单分页和筛选参数
   * @param response CSV 下载响应对象
   * @throws IOException 响应流写出失败时抛出 IOException。
   */
  public void export(@Valid OrderPageDto dto, HttpServletResponse response) throws IOException {
    List<OrderExportVo> list = orderService.platformExportVoList(dto);
    CsvExporter.write(response, "platform-orders.csv", ORDER_HEADERS, toOrderRows(list));
  }

  /**
   * 查询平台订单统计。
   *
   * <p>按筛选 DTO 聚合订单总量、金额和分状态数量。
   *
   * @param dto 订单分页和筛选参数
   * @return 订单统计统一响应。
   */
  @GetMapping("/statistics")
  public Result<OrderStatisticsVo> statistics(@Valid OrderPageDto dto) {
    return Result.success(orderService.statistics(dto));
  }

  /**
   * 查询订单经营汇总。
   *
   * <p>日期和商家参数均可空，非法格式由服务层降级。
   *
   * @param startDate 可选的 yyyy-MM-dd 开始日期
   * @param endDate 可选的 yyyy-MM-dd 结束日期
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @return 订单汇总统一响应。
   */
  @GetMapping("/summary")
  public Result<OrderSummaryVo> summary(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false) String merchantId) {
    return Result.success(orderService.orderSummary(startDate, endDate, merchantId));
  }

  /**
   * 查询订单趋势。
   *
   * <p>按日期返回单量和已支付金额点位。
   *
   * @param startDate 可选的 yyyy-MM-dd 开始日期
   * @param endDate 可选的 yyyy-MM-dd 结束日期
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @return 订单趋势统一响应。
   */
  @GetMapping("/trend")
  public Result<OrderTrendVo> trend(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false) String merchantId) {
    return Result.success(orderService.orderTrend(startDate, endDate, merchantId));
  }

  /**
   * 把订单导出视图转换为 CSV 字符串行。
   *
   * <p>空金额和空时间输出空字符串，其余字段使用字符串或纯金额文本。
   *
   * @param list 待转换的订单导出视图列表
   * @return 与表头顺序一致的 CSV 行列表。
   */
  private List<String[]> toOrderRows(List<OrderExportVo> list) {
    List<String[]> rows = new ArrayList<>(list.size());
    for (OrderExportVo vo : list) {
      rows.add(
          new String[] {
            String.valueOf(vo.getId()),
            String.valueOf(vo.getUserId()),
            String.valueOf(vo.getMerchantId()),
            String.valueOf(vo.getSkuId()),
            String.valueOf(vo.getQuantity()),
            vo.getAmount() == null ? "" : vo.getAmount().toPlainString(),
            String.valueOf(vo.getStatus()),
            vo.getCreateTime() == null ? "" : vo.getCreateTime().toString()
          });
    }
    return rows;
  }
}
