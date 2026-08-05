package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.FavoritePageDTO;
import com.yirancrazy.minimall.user.service.FavoriteService;
import com.yirancrazy.minimall.user.vo.FavoriteVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏控制器，提供商品收藏增删查 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/favorites")
public class FavoriteControllerV1 {

    private final FavoriteService favoriteService;

    public FavoriteControllerV1(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 收藏指定商品。
     * @param userId 用户ID，来自网关X-User-Id头
     * @param skuId 商品SKU ID
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{skuId}")
    public Result<Void> add(@RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
        favoriteService.addFavorite(userId, skuId);
        return Result.success(null);
    }

    /**
     * 取消收藏指定商品。
     * @param userId 用户ID，来自网关X-User-Id头
     * @param skuId 商品SKU ID
     * @return 无业务数据的成功响应
     */
    @DeleteMapping("/{skuId}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
        favoriteService.removeFavorite(userId, skuId);
        return Result.success(null);
    }

    /**
     * 分页查询当前用户收藏列表，按收藏时间倒序。
     * @param userId 用户ID，来自网关X-User-Id头
     * @param dto 分页入参
     * @return 收藏分页结果
     */
    @GetMapping
    public Result<CursorPageVO<FavoriteVO>> page(@RequestHeader("X-User-Id") Long userId, @Valid FavoritePageDTO dto) {
        return Result.success(favoriteService.pageFavorites(userId, dto));
    }

    /**
     * 统计当前用户收藏数量。
     * @param userId 用户ID，来自网关X-User-Id头
     * @return 收藏数量
     */
    @GetMapping("/count")
    public Result<Integer> count(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(favoriteService.countFavorites(userId));
    }
}
