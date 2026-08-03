package com.yirancrazy.minimall.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.user.dto.FavoritePageDTO;
import com.yirancrazy.minimall.user.vo.FavoriteVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏领域服务接口，定义商品收藏增删查业务契约
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
public interface FavoriteService {

    /**
     * 收藏商品，已存在则幂等返回。
     * @param userId 用户ID，必须 > 0
     * @param skuId 商品SKU ID，必须 > 0
     */
    void addFavorite(Long userId, Long skuId);

    /**
     * 取消收藏，不存在则幂等返回。
     * @param userId 用户ID，必须 > 0
     * @param skuId 商品SKU ID，必须 > 0
     */
    void removeFavorite(Long userId, Long skuId);

    /**
     * 分页查询用户收藏列表，按收藏时间倒序。
     * @param userId 用户ID
     * @param dto 分页入参
     * @return 收藏分页结果
     */
    IPage<FavoriteVO> pageFavorites(Long userId, FavoritePageDTO dto);

    /**
     * 统计用户收藏数量。
     * @param userId 用户ID
     * @return 收藏数量
     */
    int countFavorites(Long userId);
}
