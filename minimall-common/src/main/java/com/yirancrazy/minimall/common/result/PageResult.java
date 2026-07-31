package com.yirancrazy.minimall.common.result;

import java.util.Collections;
import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PageResult，提供公共相关能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
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
     * 构造空分页结果，用于查询无命中时的兜底返回。
     * @param <T> 记录类型
     * @return 总条数与总页数为 0、当前页为 1、每页大小为 10、记录为空列表的分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 0L, 1L, 10L, Collections.emptyList());
    }

    /**
     * 从 MyBatis-Plus 分页对象构造分页结果。
     * @param <T> 记录类型
     * @param page MyBatis-Plus 分页对象
     * @return 分页结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getPages(),
            page.getCurrent(), page.getSize(), page.getRecords());
    }
}
