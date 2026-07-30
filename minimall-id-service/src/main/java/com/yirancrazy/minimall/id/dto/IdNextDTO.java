package com.yirancrazy.minimall.id.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
* 号段下发返回 DTO，承载请求的业务标签与生成的新 ID，供内部接口调用方使用。
 */
@Data
@AllArgsConstructor
public class IdNextDTO {
    private String bizTag;
    private Long id;
}