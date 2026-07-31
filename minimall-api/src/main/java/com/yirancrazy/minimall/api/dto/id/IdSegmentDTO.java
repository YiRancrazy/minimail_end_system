package com.yirancrazy.minimall.api.dto.id;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdSegment数据传输对象，用于IdSegment相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class IdSegmentDTO {
    private String bizTag;
    private Long id;
}