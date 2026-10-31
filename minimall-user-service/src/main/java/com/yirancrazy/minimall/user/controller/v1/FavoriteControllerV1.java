package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.FavoritePageDto;
import com.yirancrazy.minimall.user.service.FavoriteService;
import com.yirancrazy.minimall.user.vo.FavoriteVo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏 HTTP 入口，基于 X-User-Id 请求头提供收藏新增、取消、游标分页和数量查询，不接收客户端用户身份参数.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/favorites")
public class FavoriteControllerV1 {

  private final FavoriteService favoriteService;

  /**
   * 注入用户收藏业务服务。
   *
   * @param favoriteService 收藏服务
   */
  public FavoriteControllerV1(FavoriteService favoriteService) {
    this.favoriteService = favoriteService;
  }

  /**
   * 为当前用户新增商品收藏。
   *
   * @param userId 从 X-User-Id 获取的当前用户 ID
   * @param skuId 路径中的 SKU ID
   * @return data 为 null 的成功响应
   * @throws com.yirancrazy.minimall.common.exception.BizException 用户 ID 或 SKU ID 非法时抛出
   */
  @PostMapping("/{skuId}")
  public Result<Void> add(
      @RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
    favoriteService.addFavorite(userId, skuId);
    return Result.success(null);
  }

  /**
   * 取消当前用户对指定 SKU 的收藏。
   *
   * <p>目标收藏不存在时仍返回成功，保持删除语义幂等。
   *
   * @param userId 从 X-User-Id 获取的当前用户 ID
   * @param skuId 路径中的 SKU ID
   * @return data 为 null 的成功响应
   * @throws com.yirancrazy.minimall.common.exception.BizException 用户 ID 或 SKU ID 非法时抛出
   */
  @DeleteMapping("/{skuId}")
  public Result<Void> remove(
      @RequestHeader("X-User-Id") Long userId, @PathVariable("skuId") Long skuId) {
    favoriteService.removeFavorite(userId, skuId);
    return Result.success(null);
  }

  /**
   * 按游标分页查询当前用户的收藏。
   *
   * @param userId 从 X-User-Id 获取的当前用户 ID
   * @param dto 游标和每页数量
   * @return 已补充可用商品展示信息的收藏分页结果
   */
  @GetMapping
  public Result<CursorPageVo<FavoriteVo>> page(
      @RequestHeader("X-User-Id") Long userId, @Valid FavoritePageDto dto) {
    return Result.success(favoriteService.pageFavorites(userId, dto));
  }

  /**
   * 查询当前用户的收藏数量。
   *
   * @param userId 从 X-User-Id 获取的当前用户 ID
   * @return 收藏记录数量
   */
  @GetMapping("/count")
  public Result<Integer> count(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(favoriteService.countFavorites(userId));
  }
}
