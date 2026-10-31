package com.yirancrazy.minimall.stock.vo;

import com.yirancrazy.minimall.stock.entity.StockPo;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向接口输出 SKU 的商户归属、可用量、预占量和告警阈值。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class StockVo {

  private Long id;
  private Long skuId;
  private Long merchantId;
  private Long available;
  private Long reserved;
  private Long alertThreshold;

  /**
   * 将库存持久化对象转换为接口视图，不修改库存统计值。
   *
   * @param po 库存持久化对象
   * @return 与库存记录字段对应的视图对象
   */
  public static StockVo from(StockPo po) {
    StockVo vo = new StockVo();
    vo.setId(po.getId());
    vo.setSkuId(po.getSkuId());
    vo.setMerchantId(po.getMerchantId());
    vo.setAvailable(po.getAvailable());
    vo.setReserved(po.getReserved());
    vo.setAlertThreshold(po.getAlertThreshold());
    return vo;
  }
}
