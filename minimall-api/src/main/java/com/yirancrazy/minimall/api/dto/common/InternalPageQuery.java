package com.yirancrazy.minimall.api.dto.common;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理内部接口共用的游标分页与关键字筛选条件.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class InternalPageQuery {

    private String cursor;

    private Integer limit;

    private String keyword;

    private Integer status;
}
