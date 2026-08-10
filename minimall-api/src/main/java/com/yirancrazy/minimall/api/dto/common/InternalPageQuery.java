package com.yirancrazy.minimall.api.dto.common;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 跨服务内部查询通用 DTO，避免 minimall-api 反向依赖 minimall-common 业务 DTO
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class InternalPageQuery {

    /** 游标 */
    private String cursor;

    /** 每页条数 */
    private Integer limit;

    /** 模糊匹配关键词 */
    private String keyword;

    /** 状态过滤（如审核状态），null 表示不过滤 */
    private Integer status;
}
