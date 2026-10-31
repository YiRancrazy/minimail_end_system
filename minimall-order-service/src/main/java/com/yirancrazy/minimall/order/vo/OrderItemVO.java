package com.yirancrazy.minimall.order.vo;

import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.entity.OrderItemPo;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单明细展示所需的 SPU、SKU、数量和金额信息。
 *   业务边界：商品图片可经 MinioUtil 转换为公开 URL，SPU 名称可能由服务层补充。
 *   使用场景：用户订单详情和列表回显使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderItemVo {

  private Long spuId;
  private Long skuId;

  private String spuName;

  private String skuName;

  private String skuImageUrl;
  private Integer quantity;

  private String unitPrice;

  private String amount;

  /**
   * 把订单持久化对象转换为展示视图。
   *
   * <p>复制展示字段并转换金额、状态或图片地址；不查询数据库。
   *
   * @param po 订单明细持久化对象
   * @return 转换后的视图对象。
   */
  public static OrderItemVo from(OrderItemPo po) {
    return from(po, null);
  }

  /**
   * 把订单持久化对象转换为展示视图。
   *
   * <p>复制展示字段并转换金额、状态或图片地址；不查询数据库。
   *
   * @param po 订单明细持久化对象
   * @param minioUtil SKU 图片对象地址解析工具
   * @return 转换后的视图对象。
   */
  public static OrderItemVo from(OrderItemPo po, MinioUtil minioUtil) {
    OrderItemVo vo = new OrderItemVo();
    vo.setSpuId(po.getSpuId());
    vo.setSkuId(po.getSkuId());
    vo.setSkuName(po.getSkuName());
    vo.setSkuImageUrl(
        minioUtil == null ? po.getSkuImageUrl() : minioUtil.resolvePublicUrl(po.getSkuImageUrl()));
    vo.setQuantity(po.getQuantity());
    vo.setUnitPrice(po.getUnitPrice() == null ? null : po.getUnitPrice().toPlainString());
    vo.setAmount(po.getAmount() == null ? null : po.getAmount().toPlainString());
    return vo;
  }
}
