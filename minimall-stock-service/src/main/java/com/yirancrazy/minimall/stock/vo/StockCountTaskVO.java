package com.yirancrazy.minimall.stock.vo;

import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向平台接口输出盘点任务状态、账面量、实盘量和差异。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class StockCountTaskVo {

  private Long id;
  private Long skuId;
  private Long expectedQuantity;
  private Long actualQuantity;
  private Long diffQuantity;
  private Integer status;
  private String remark;
  private Long operatorId;
  private LocalDateTime createTime;

  /**
   * 将盘点任务持久化对象转换为接口视图，并复制创建时间等展示字段。
   *
   * @param po 盘点任务持久化对象
   * @return 与持久化对象字段对应的盘点任务视图
   */
  public static StockCountTaskVo from(StockCountTaskPo po) {
    StockCountTaskVo vo = new StockCountTaskVo();
    vo.setId(po.getId());
    vo.setSkuId(po.getSkuId());
    vo.setExpectedQuantity(po.getExpectedQuantity());
    vo.setActualQuantity(po.getActualQuantity());
    vo.setDiffQuantity(po.getDiffQuantity());
    vo.setStatus(po.getStatus());
    vo.setRemark(po.getRemark());
    vo.setOperatorId(po.getOperatorId());
    vo.setCreateTime(po.getCreateTime());
    return vo;
  }
}
