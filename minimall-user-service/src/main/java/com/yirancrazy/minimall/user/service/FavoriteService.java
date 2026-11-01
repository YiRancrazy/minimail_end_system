package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.user.dto.FavoritePageDto;
import com.yirancrazy.minimall.user.vo.FavoriteVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏业务接口，定义收藏增删、游标分页富化和数量统计能力，商品快照缺失时允许降级展示.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface FavoriteService {

    /**
     * 新增用户商品收藏。
     *
     * <p>用户 ID 和 SKU ID 必须为正数；同一用户重复收藏同一 SKU 时直接返回，不重复写入。
     *
     * @param userId 用户 ID
     * @param skuId SKU ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 任一 ID 非法时抛出
     */
    void addFavorite(Long userId, Long skuId);

    /**
     * 取消用户对指定 SKU 的收藏。
     *
     * <p>按用户 ID 和 SKU ID 条件删除；收藏不存在时保持幂等，不抛业务异常。
     *
     * @param userId 用户 ID
     * @param skuId SKU ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 任一 ID 非法时抛出
     */
    void removeFavorite(Long userId, Long skuId);

    /**
     * 按游标分页查询用户收藏并补充商品展示信息。
     *
     * <p>收藏按主键倒序读取；仅富化当前页记录。商品快照缺失时保留收藏关系，商品字段为空。
     *
     * @param userId 用户 ID
     * @param dto 游标、每页数量等分页参数
     * @return 富化后的收藏游标分页结果
     */
    CursorPageVo<FavoriteVo> pageFavorites(Long userId, FavoritePageDto dto);

    /**
     * 统计指定用户当前收藏的商品数量。
     *
     * @param userId 用户 ID
     * @return 收藏记录数量；无记录时为 0
     */
    int countFavorites(Long userId);
}
