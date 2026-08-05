package com.yirancrazy.minimall.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 游标分页基础入参，支持 cursor + limit 模式，不可跳页。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class CursorPageDTO {

    /** 游标，首页传 null，后续传上一次返回的 nextCursor */
    private String cursor;

    /** 每页条数，默认 20，最大 100 */
    @Min(value = 1, message = "limit must be >= 1")
    @Max(value = 100, message = "limit must be <= 100")
    private Integer limit = 20;
}
