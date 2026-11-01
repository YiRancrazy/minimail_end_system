package com.yirancrazy.minimall.common.result;

import com.yirancrazy.minimall.common.util.CursorUtils;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于末条记录 ID 的游标分页响应，包含当前页记录、下一页游标和是否还有更多数据。
 * 游标由 CursorUtils 编码，适用于按稳定递增 ID 排序的查询结果。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageVo<T> {

    private List<T> records;

    private String nextCursor;

    private boolean hasMore;

    private int limit;

    /**
     * 创建不包含记录且没有下一页的空分页结果。
     *
     * @param <T> 记录类型
     * @param limit 本页请求条数
     * @return 空记录、无游标且 hasMore=false 的分页对象
     */
    public static <T> CursorPageVo<T> empty(int limit) {
        return new CursorPageVo<>(Collections.emptyList(), null, false, limit);
    }

    /**
     * 根据查询结果构建游标分页。调用方应多取一条记录用于判断 hasMore；
     * 有下一页时截取前 limit 条，并将截取后最后一条记录的 ID 编码为游标。
     *
     * @param <T> 记录类型
     * @param records 查询结果，建议包含 limit + 1 条
     * @param limit 本页最大记录数
     * @param lastIdGetter 从记录中提取稳定排序 ID 的函数
     * @return 构建后的游标分页；无记录时返回空分页
     */
    public static <T> CursorPageVo<T> of(List<T> records, int limit, Function<T, Long> lastIdGetter) {
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
        return new CursorPageVo<>(pageRecords, nextCursor, hasMore, limit);
    }

    /**
     * 保持分页游标、hasMore 和 limit 不变，将当前页每条记录转换为目标类型。
     *
     * @param <R> 转换后的记录类型
     * @param mapper 单条记录转换函数
     * @return 转换后的分页对象
     */
    public <R> CursorPageVo<R> map(Function<T, R> mapper) {
        List<R> mapped = records.stream().map(mapper).toList();
        return new CursorPageVo<>(mapped, nextCursor, hasMore, limit);
    }
}
