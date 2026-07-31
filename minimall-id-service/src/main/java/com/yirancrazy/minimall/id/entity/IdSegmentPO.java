package com.yirancrazy.minimall.id.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdSegmentPO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class IdSegmentPO extends BasePO {
    private String bizTag;
    private Long currentMax;
    private Long step;
    private Integer version;
}