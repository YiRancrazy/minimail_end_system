package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.user.dto.FavoritePageDTO;
import com.yirancrazy.minimall.user.vo.FavoriteVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: FavoriteService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface FavoriteService {


    /**
     * 执行 addFavorite 方法。
     * @param userId userId 参数
     * @param skuId skuId 参数
     */
    void addFavorite(Long userId, Long skuId);


    /**
     * 删除业务数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     */
    void removeFavorite(Long userId, Long skuId);


    /**
     * 分页查询数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<FavoriteVO> pageFavorites(Long userId, FavoritePageDTO dto);


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 业务数值
     */
    int countFavorites(Long userId);
}
