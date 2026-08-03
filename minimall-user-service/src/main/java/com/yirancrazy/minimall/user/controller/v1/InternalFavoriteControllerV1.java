package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.FavoriteService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏内部控制器，供其他服务通过Feign调用收藏能力
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@RestController
@RequestMapping("/internal/favorites")
public class InternalFavoriteControllerV1 {

    private final FavoriteService favoriteService;

    public InternalFavoriteControllerV1(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 收藏指定商品，供跨服务调用。
     * @param userId 用户ID
     * @param skuId 商品SKU ID
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{userId}/{skuId}")
    public Result<Void> addFavorite(@PathVariable Long userId, @PathVariable Long skuId) {
        favoriteService.addFavorite(userId, skuId);
        return Result.success(null);
    }
}
