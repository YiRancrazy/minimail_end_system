package com.yirancrazy.minimall.id.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: t_id_segment 的持久化模型。保存数据库号段策略使用的业务标签、已分配上限
 *     和步长；BasePo 提供标识、审计、软删除和乐观锁字段，当前 IdService 不读取该表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_id_segment")
public class IdSegmentPo extends BasePo {
  private String bizTag; // 业务标签，用于区分不同业务场景（如订单、支付、库存等）
  private Long currentMax; // 当前已分配的最大ID值（号段的截止值）
  private Long step; // 号段步长，即每次取号时预分配的数量
  // version 乐观锁版本号由 BasePo 统一提供（@Version），此处不再重复声明以免遮蔽父类注解
}
