package com.yirancrazy.minimall.id.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
* 号段持久化实体，对应 id_segment 表，记录各业务标签当前最大号与步长等元信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdSegmentPO extends BasePO {
    private String bizTag;
    private Long currentMax;
    private Long step;
    private Integer version;
}