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
 * @Description: FavoriteControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/user/favorites")
public class FavoriteControllerV1 {

    private final FavoriteService favoriteService;

    /**
     * 构造 FavoriteControllerV1 实例。
     * @param favoriteService favoriteService 参数
     */
    public FavoriteControllerV1(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }


    /**
     * 执行 add 方法。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{skuId}")
    public Result<Void> add(@RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
        favoriteService.addFavorite(userId, skuId);
        return Result.success(null);
    }


    /**
     * 删除业务数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{skuId}")
    public Result<Void> remove(@RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
        favoriteService.removeFavorite(userId, skuId);
        return Result.success(null);
    }


    /**
     * 分页查询数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<FavoriteVO>> page(@RequestHeader("X-User-Id") Long userId, @Valid FavoritePageDTO dto) {
        return Result.success(favoriteService.pageFavorites(userId, dto));
    }


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/count")
    public Result<Integer> count(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(favoriteService.countFavorites(userId));
    }
}
