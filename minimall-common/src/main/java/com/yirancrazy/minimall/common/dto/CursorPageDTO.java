package com.yirancrazy.minimall.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CursorPageDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class CursorPageDTO {


    private String cursor;


    @Min(value = 1, message = "limit must be >= 1")
    @Max(value = 100, message = "limit must be <= 100")
    private Integer limit = 20;
}
