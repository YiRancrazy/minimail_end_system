package com.yirancrazy.minimall.goods.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品审核记录持久化对象，映射 t_goods_audit_record 表
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_goods_audit_record")
public class SpuAuditRecordPO extends BasePO {

    private Long spuId;
    private Long auditorId;
    private Integer decision;
    private String reason;
    private LocalDateTime auditAt;
}
