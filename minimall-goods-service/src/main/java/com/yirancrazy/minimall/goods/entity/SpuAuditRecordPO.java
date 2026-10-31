package com.yirancrazy.minimall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_goods_audit_record 表，保存商品审核决策、审核人和原因。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_goods_audit_record")
public class SpuAuditRecordPo extends BasePo {

  private Long spuId;
  private Long auditorId;
  private Integer decision;
  private String reason;
  private LocalDateTime auditAt;
}
