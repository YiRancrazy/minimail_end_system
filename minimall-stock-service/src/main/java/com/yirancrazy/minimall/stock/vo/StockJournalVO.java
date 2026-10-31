package com.yirancrazy.minimall.stock.vo;

import com.yirancrazy.minimall.stock.entity.StockJournalPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向商户和平台接口输出库存流水的变动方向、类型及关联订单。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class StockJournalVo {

  private Long id;
  private Long skuId;
  private Long quantity;
  private Integer type;
  private String reason;
  private String orderNo;
  private LocalDateTime createTime;

  /**
   * 将库存流水持久化对象转换为适合接口序列化的视图。
   *
   * @param po 库存流水持久化对象
   * @return 与流水记录字段对应的视图对象
   */
  public static StockJournalVo from(StockJournalPo po) {
    StockJournalVo vo = new StockJournalVo();
    vo.setId(po.getId());
    vo.setSkuId(po.getSkuId());
    vo.setQuantity(po.getQuantity());
    vo.setType(po.getType());
    vo.setReason(po.getReason());
    vo.setOrderNo(po.getOrderNo());
    vo.setCreateTime(po.getCreateTime());
    return vo;
  }
}
