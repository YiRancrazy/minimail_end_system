package com.yirancrazy.minimall.common.result;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.yirancrazy.minimall.common.util.CursorUtils;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CursorPageVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageVO<T> {


    private List<T> records;


    private String nextCursor;


    private boolean hasMore;


    private int limit;


    /**
     * 执行 empty 方法。
     * @param <T> 泛型参数
     * @param limit limit 参数
     * @return 处理结果
     */
    public static <T> CursorPageVO<T> empty(int limit) {
        return new CursorPageVO<>(Collections.emptyList(), null, false, limit);
    }


    /**
     * 执行 of 方法。
     * @param <T> 泛型参数
     * @param records records 参数
     * @param limit limit 参数
     * @param lastIdGetter lastIdGetter 参数
     * @return 处理结果
     */
    public static <T> CursorPageVO<T> of(List<T> records, int limit,
                                         Function<T, Long> lastIdGetter) {
        if (records == null || records.isEmpty()) {
            return empty(limit);
        }
        boolean hasMore = records.size() > limit;
        List<T> pageRecords = hasMore ? records.subList(0, limit) : records;
        String nextCursor = null;
        if (hasMore) {
            T lastItem = pageRecords.get(pageRecords.size() - 1);
            nextCursor = CursorUtils.encode(lastIdGetter.apply(lastItem));
        }
        return new CursorPageVO<>(pageRecords, nextCursor, hasMore, limit);
    }


    /**
     * 执行 map 方法。
     * @param <R> 泛型参数
     * @param mapper mapper 参数
     * @return 处理结果
     */
    public <R> CursorPageVO<R> map(Function<T, R> mapper) {
        List<R> mapped = records.stream().map(mapper).toList();
        return new CursorPageVO<>(mapped, nextCursor, hasMore, limit);
    }
}
