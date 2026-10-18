package com.yirancrazy.minimall.common.result;

import java.util.Collections;
import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PageResult 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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
     * 执行 empty 方法。
     * @param <T> 泛型参数
     * @return 统一响应结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 0L, 1L, 10L, Collections.emptyList());
    }


    /**
     * 执行 of 方法。
     * @param <T> 泛型参数
     * @param page page 参数
     * @return 统一响应结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getPages(),
            page.getCurrent(), page.getSize(), page.getRecords());
    }
}