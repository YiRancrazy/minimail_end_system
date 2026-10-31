package com.yirancrazy.minimall.order.vo;

import com.yirancrazy.minimall.order.entity.OrderPo;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载用户、商家和平台订单详情及列表展示数据。
 *   业务边界：金额转为字符串，状态转为 alias，可附带明细、商品名称和退款拒绝原因；不暴露内部版本或库存释放字段。
 *   使用场景：订单查询控制器通过转换方法返回该视图。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderVo {

  private Long id;
  private String orderNo;
  private Long userId;
  private Long merchantId;
  private Long skuId;

  private String skuName;
  private Integer quantity;

  private String amount;

  private String status;

  private String addressSnapshot;
  private LocalDateTime createTime;
  private LocalDateTime shippedAt;

  private String rejectReason;

  private List<OrderItemVo> items;

  /**
   * 把订单持久化对象转换为展示视图。
   *
   * <p>复制展示字段并转换金额、状态或图片地址；不查询数据库。
   *
   * @param po 订单持久化对象
   * @return 转换后的视图对象。
   */
  public static OrderVo from(OrderPo po) {
    OrderVo vo = new OrderVo();
    vo.setId(po.getId());
    vo.setOrderNo(po.getOrderNo());
    vo.setUserId(po.getUserId());
    vo.setMerchantId(po.getMerchantId());
    vo.setSkuId(po.getSkuId());
    vo.setQuantity(po.getQuantity());
    vo.setAmount(po.getAmount() == null ? null : po.getAmount().toPlainString());
    vo.setStatus(statusAlias(po.getStatus()));
    vo.setAddressSnapshot(po.getReceiverSnapshotJson());
    vo.setCreateTime(po.getCreateTime());
    vo.setShippedAt(po.getShippedAt());
    return vo;
  }

  /**
   * 把订单状态整数编码转换为稳定 alias。
   *
   * <p>已知状态返回枚举别名，未知或空编码按字符串返回。
   *
   * @param code 订单状态整数编码
   * @return 状态别名或原编码字符串。
   */
  private static String statusAlias(Integer code) {
    for (com.yirancrazy.minimall.order.constant.OrderStatusEnum e :
        com.yirancrazy.minimall.order.constant.OrderStatusEnum.values()) {
      if (e.getCode().equals(String.valueOf(code))) {
        return e.getAlias();
      }
    }
    return String.valueOf(code);
  }
}
