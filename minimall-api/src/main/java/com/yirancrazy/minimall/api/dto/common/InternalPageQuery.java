package com.yirancrazy.minimall.api.dto.common;

import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalPageQuery 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class InternalPageQuery {


    private String cursor;


    private Integer limit;


    private String keyword;


    private Integer status;
}
