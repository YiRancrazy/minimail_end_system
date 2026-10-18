package com.yirancrazy.minimall.goods.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuAuditRecordPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
