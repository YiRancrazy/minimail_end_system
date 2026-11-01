package com.yirancrazy.minimall.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 游标分页查询参数，携带上一页返回的游标和本页期望条数。
 * limit 默认 20，并在 Bean Validation 层限制为 1 到 100。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CursorPageDto {

    private String cursor;

    @Min(value = 1, message = "limit must be >= 1")
    @Max(value = 100, message = "limit must be <= 100")
    private Integer limit = 20;
}
