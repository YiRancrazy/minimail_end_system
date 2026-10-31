package com.yirancrazy.minimall.goods.vo;

import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 输出商品审核记录及由实体到视图的字段映射。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class SpuAuditRecordVo {

  private Long id;
  private Long spuId;
  private Long auditorId;
  private Integer decision;
  private String reason;
  private LocalDateTime auditAt;

  /**
   * 将商品审核记录实体映射为视图对象。
   *
   * @param po 待转换的持久化对象
   * @return 商品审核记录视图对象
   */
  public static SpuAuditRecordVo from(SpuAuditRecordPo po) {
    SpuAuditRecordVo vo = new SpuAuditRecordVo();
    vo.setId(po.getId());
    vo.setSpuId(po.getSpuId());
    vo.setAuditorId(po.getAuditorId());
    vo.setDecision(po.getDecision());
    vo.setReason(po.getReason());
    vo.setAuditAt(po.getAuditAt());
    return vo;
  }
}
