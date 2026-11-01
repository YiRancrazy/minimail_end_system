package com.yirancrazy.minimall.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 传统页码分页响应，承载总记录数、总页数、当前页、页大小和当前页数据，
 * 可由 MyBatis-Plus IPage 直接转换。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    private List<T> records;

    /**
     * 创建无记录的分页结果，当前页为 1、页大小为 10、总数和总页数均为 0。
     *
     * @param <T> 记录类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 0L, 1L, 10L, Collections.emptyList());
    }

    /**
     * 将 MyBatis-Plus 分页对象转换为接口响应结构。
     *
     * @param <T> 记录类型
     * @param page MyBatis-Plus 分页查询结果
     * @return 包含统计信息和当前页记录的 PageResult
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(
                page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
