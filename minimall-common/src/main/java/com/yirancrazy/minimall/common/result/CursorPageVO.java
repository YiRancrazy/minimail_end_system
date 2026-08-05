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
 * @Description: 游标分页响应体，包含记录列表和下一页游标信息。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageVO<T> {

    /** 当前页记录列表 */
    private List<T> records;

    /** 下一页游标，无更多数据时为 null */
    private String nextCursor;

    /** 是否还有更多数据 */
    private boolean hasMore;

    /** 每页条数 */
    private int limit;

    /**
     * 构造空游标分页结果。
     * @param <T> 记录类型
     * @param limit 每页条数
     * @return 空分页结果
     */
    public static <T> CursorPageVO<T> empty(int limit) {
        return new CursorPageVO<>(Collections.emptyList(), null, false, limit);
    }

    /**
     * 根据查询结果列表构造游标分页响应，自动截断至 limit 条并计算 nextCursor。
     * @param <T> 记录类型
     * @param records 查询结果（可能包含 limit+1 条用于判断 hasMore）
     * @param limit 每页条数
     * @param lastIdGetter 获取记录ID的函数
     * @return 游标分页响应
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
     * 将分页记录从 PO 类型映射为 VO 类型，保留游标与分页元信息。
     * @param <R> 目标记录类型
     * @param mapper PO → VO 转换函数
     * @return 映射后的游标分页响应
     */
    public <R> CursorPageVO<R> map(Function<T, R> mapper) {
        List<R> mapped = records.stream().map(mapper).toList();
        return new CursorPageVO<>(mapped, nextCursor, hasMore, limit);
    }
}
