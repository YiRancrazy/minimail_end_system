package com.yirancrazy.minimall.api.dto.id;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 号段 DTO，承载当前可用号段范围与步长。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
public class IdSegmentDTO {
    private String bizTag;
    private Long id;
}