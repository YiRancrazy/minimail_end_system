package com.yirancrazy.minimall.id.entity;

import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IdSegmentPO extends BasePO {
    private String bizTag;
    private Long currentMax;
    private Long step;
    private Integer version;
}