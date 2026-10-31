package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.FavoriteService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收藏内部服务入口，供可信内部调用按路径中的用户 ID 和 SKU ID 创建收藏；当前路径不得直接暴露公网.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/favorites")
public class InternalFavoriteControllerV1 {

  private final FavoriteService favoriteService;

  /**
   * 注入用户收藏业务服务。
   *
   * @param favoriteService 收藏服务
   */
  public InternalFavoriteControllerV1(FavoriteService favoriteService) {
    this.favoriteService = favoriteService;
  }

  /**
   * 为内部调用指定的用户新增商品收藏。
   *
   * <p>用户身份由内部调用路径显式传入；重复收藏由收藏服务按幂等语义处理。
   *
   * @param userId 目标用户 ID
   * @param skuId SKU ID
   * @return data 为 null 的成功响应
   * @throws com.yirancrazy.minimall.common.exception.BizException 任一 ID 非法时抛出
   */
  @PostMapping("/{userId}/{skuId}")
  public Result<Void> addFavorite(@PathVariable Long userId, @PathVariable Long skuId) {
    favoriteService.addFavorite(userId, skuId);
    return Result.success(null);
  }
}
