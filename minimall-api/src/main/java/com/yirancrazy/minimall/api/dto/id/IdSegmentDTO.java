package com.yirancrazy.minimall.api.dto.id;

import lombok.Data;

/**
* 号段 DTO，承载当前可用号段范围与步长。
 */
@Data
public class IdSegmentDTO {
    private String bizTag;
    private Long id;
}