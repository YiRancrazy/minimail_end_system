package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.FavoriteService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalFavoriteControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/favorites")
public class InternalFavoriteControllerV1 {

    private final FavoriteService favoriteService;

    /**
     * 构造 InternalFavoriteControllerV1 实例。
     * @param favoriteService favoriteService 参数
     */
    public InternalFavoriteControllerV1(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }


    /**
     * 执行 addFavorite 方法。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{userId}/{skuId}")
    public Result<Void> addFavorite(@PathVariable Long userId, @PathVariable Long skuId) {
        favoriteService.addFavorite(userId, skuId);
        return Result.success(null);
    }
}
