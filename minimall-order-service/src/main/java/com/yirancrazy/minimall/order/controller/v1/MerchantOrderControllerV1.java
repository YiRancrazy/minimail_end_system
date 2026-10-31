package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.dto.OrderShipDto;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供商家端订单分页、详情、发货、关闭、待办计数、退款审核和 CSV 导出接口。
 *   业务边界：只处理 HTTP 参数与响应格式，订单归属及状态规则委托 OrderService；商家身份来自请求头上下文。
 *   使用场景：商家工作台通过 /api/v1/merchant/orders 调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/orders")
public class MerchantOrderControllerV1 {

  private static final String[] ORDER_HEADERS = {
    "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
  };

  private final OrderService orderService;

  /**
   * 创建商家订单控制器并注入订单服务。
   *
   * <p>控制器不持有可变状态，订单操作统一委托 OrderService。
   *
   * @param orderService 订单业务服务
   */
  public MerchantOrderControllerV1(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * 查询商家订单游标页。
   *
   * <p>请求头商家 ID 会覆盖筛选字段，并补全明细汇总与 SKU 信息。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param dto 订单分页和筛选参数
   * @return 订单视图游标页统一响应。
   */
  @GetMapping
  public Result<CursorPageVo<OrderVo>> page(
      @RequestHeader("X-Merchant-Id") Long merchantId, @Valid OrderPageDto dto) {
    dto.setMerchantId(merchantId);
    return Result.success(orderService.merchantPageVo(dto));
  }

  /**
   * 查询商家订单详情。
   *
   * <p>订单必须属于请求头指定商家。
   *
   * @param orderId 订单 ID
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @return 订单详情统一响应。
   */
  @GetMapping("/{orderId}")
  public Result<OrderVo> detail(
      @PathVariable Long orderId, @RequestHeader("X-Merchant-Id") Long merchantId) {
    return Result.success(orderService.merchantDetailVo(orderId, merchantId));
  }

  /**
   * 提交商家发货。
   *
   * <p>路径订单 ID、商家身份和承运信息交由订单服务校验。
   *
   * @param orderId 订单 ID
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param dto 商家发货请求体
   * @return 空数据的成功响应。
   * @throws BizException 订单、商家或状态不满足发货条件。
   */
  @PostMapping("/{orderId}/ship")
  public Result<Void> ship(
      @PathVariable Long orderId,
      @RequestHeader("X-User-Id") Long merchantId,
      @Valid @RequestBody OrderShipDto dto) {
    orderService.ship(orderId, merchantId, dto.getCarrier(), dto.getTrackingNo());
    return Result.success(null);
  }

  /**
   * 提交商家关闭订单。
   *
   * <p>关闭后由订单服务尝试释放库存并记录补偿状态。
   *
   * @param orderId 订单 ID
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @return 空数据的成功响应。
   * @throws BizException 商家不匹配或订单状态不允许关闭。
   */
  @PostMapping("/{orderId}/close")
  public Result<Void> merchantClose(
      @PathVariable Long orderId, @RequestHeader("X-Merchant-Id") Long merchantId) {
    orderService.merchantClose(orderId, merchantId);
    return Result.success(null);
  }

  /**
   * 查询商家待办订单数量。
   *
   * <p>只统计待支付、已支付和退款中订单。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @return 计数的统一响应。
   */
  @GetMapping("/_count")
  public Result<Long> pendingCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
    return Result.success(orderService.pendingCount(merchantId));
  }

  /**
   * 提交商家退款审核结果。
   *
   * <p>approved=true 通过并请求支付退款，false 驳回并回退状态。
   *
   * @param orderId 订单 ID
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param approved true 表示通过，false 表示驳回
   * @return 空数据的成功响应。
   */
  @PostMapping("/{orderId}/refund-review")
  public Result<Void> refundReview(
      @PathVariable Long orderId,
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @RequestParam boolean approved) {
    orderService.reviewRefund(orderId, approved, merchantId, null);
    return Result.success(null);
  }

  /**
   * 导出商家订单 CSV。
   *
   * <p>按请求头和筛选条件查询，并写入固定表头和订单文件。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param dto 订单分页和筛选参数
   * @param response CSV 下载响应对象
   * @throws IOException 响应流写出失败时抛出 IOException。
   */
  public void export(
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @Valid OrderPageDto dto,
      HttpServletResponse response)
      throws IOException {
    List<OrderExportVo> list = orderService.exportVoList(merchantId, dto);
    CsvExporter.write(response, "orders.csv", ORDER_HEADERS, toOrderRows(list));
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
