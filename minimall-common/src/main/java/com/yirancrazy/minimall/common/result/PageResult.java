package com.yirancrazy.minimall.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 分页响应对象，封装记录列表与总条数、总页数、当前页、每页大小等分页元数据，并支持由 MyBatis-Plus 的 IPage 直接转换。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    private List<T> records;

    /**
     * 构造空分页结果，用于查询无命中时的兜底返回。
     *
     * @return 总条数与总页数为 0、当前页为 1、每页大小为 10、记录为空列表的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 0L, 1L, 10L, Collections.emptyList());
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getPages(),
            page.getCurrent(), page.getSize(), page.getRecords());
    }
}