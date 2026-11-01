package com.yirancrazy.minimall.api.dto.id;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按业务标签返回的分布式唯一 ID 结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class IdSegmentDto {
    private String bizTag;
    private Long id;
}
