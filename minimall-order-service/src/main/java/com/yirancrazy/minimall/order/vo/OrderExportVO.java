package com.yirancrazy.minimall.order.vo;

import com.yirancrazy.minimall.order.entity.OrderPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家或平台订单 CSV 导出所需的扁平字段。
 *   业务边界：只转换订单主记录，不包含明细列表；金额保持 BigDecimal 由控制器格式化。
 *   使用场景：OrderServiceImpl 导出查询后转换为 CSV 行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderExportVo {

  private Long id;
  private Long userId;
  private Long merchantId;
  private Long skuId;
  private Integer quantity;
  private BigDecimal amount;
  private Integer status;
  private LocalDateTime createTime;

  /**
   * 把订单持久化对象转换为展示视图。
   *
   * <p>复制展示字段并转换金额、状态或图片地址；不查询数据库。
   *
   * @param po 订单持久化对象
   * @return 转换后的视图对象。
   */
  public static OrderExportVo from(OrderPo po) {
    OrderExportVo vo = new OrderExportVo();
    vo.setId(po.getId());
    vo.setUserId(po.getUserId());
    vo.setMerchantId(po.getMerchantId());
    vo.setSkuId(po.getSkuId());
    vo.setQuantity(po.getQuantity());
    vo.setAmount(po.getAmount());
    vo.setStatus(po.getStatus());
    vo.setCreateTime(po.getCreateTime());
    return vo;
  }
}
