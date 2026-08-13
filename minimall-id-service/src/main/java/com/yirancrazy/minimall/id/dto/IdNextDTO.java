package com.yirancrazy.minimall.id.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdNext数据传输对象，用于IdNext相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
@AllArgsConstructor
public class IdNextDTO {
    private String bizTag;  // 业务标签
    private Long id;        // id
}