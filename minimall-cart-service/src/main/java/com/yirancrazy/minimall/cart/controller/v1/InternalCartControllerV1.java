package com.yirancrazy.minimall.cart.controller.v1;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.cart.dto.CartCountDto;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.result.Result;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 /internal/cart 下提供购物车计数、结算快照和结算清理服务间接口。请求应首先通过
 *     InternalAuthFilter；控制器只负责传输层校验，并将归属、勾选和清理规则委托给 CartService。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/cart")
public class InternalCartControllerV1 {

  private final CartService cartService;

  /**
   * 创建购物车内部接口控制器.
   *
   * @param cartService 负责购物车业务规则的应用服务
   */
  public InternalCartControllerV1(CartService cartService) {
    this.cartService = cartService;
  }

  /**
   * 统计指定内部用户的购物车条目数量.
   *
   * @param dto 已校验且包含必填用户标识的查询对象
   * @return 包含用户购物车条目数量的统一响应
   */
  @GetMapping("/count")
  public Result<Long> count(@Valid CartCountDto dto) {
    return Result.success(cartService.countByUser(dto.getUserId()));
  }

  /**
   * 返回指定购物车条目的权威结算快照.
   *
   * <p>服务层要求每个请求条目都属于指定用户且处于勾选状态；重复主键按首次出现位置去重后保留
   * 请求顺序。
   *
   * @param dto 已校验的用户标识和购物车条目主键
   * @return 包含购物车条目主键、SKU 主键和数量的统一响应
   */
  @PostMapping("/checkout-snapshot")
  public Result<List<CartCheckoutItemDto>> checkoutSnapshot(
      @Valid @RequestBody CartCheckoutQueryDto dto) {
    return Result.success(cartService.getCheckoutItems(dto.getUserId(), dto.getItemIds()));
  }

  /**
   * 执行订单流程传入的购物车结算清理.
   *
   * @param dto 已校验的用户标识和已结算条目数量
   * @return 本地扣减或删除完成后的统一成功响应
   */
  @PostMapping("/checkout-cleanup")
  public Result<Void> checkoutCleanup(@Valid @RequestBody CartCheckoutCleanupDto dto) {
    cartService.cleanupCheckout(dto.getUserId(), dto.getItems());
    return Result.success();
  }
}
