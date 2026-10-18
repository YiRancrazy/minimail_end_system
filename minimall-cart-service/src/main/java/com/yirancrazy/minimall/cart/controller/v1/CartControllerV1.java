package com.yirancrazy.minimall.cart.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartMoveToFavoriteDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.cart.vo.CartItemVO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/cart")
public class CartControllerV1 {

    private final CartService cartService;

    /**
     * 构造 CartControllerV1 实例。
     * @param cartService cartService 参数
     */
    public CartControllerV1(CartService cartService) {
        this.cartService = cartService;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<List<CartItemVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.listByUser(userId));
    }


    /**
     * 执行 add 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> add(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CartItemAddDTO dto) {
        return Result.success(cartService.add(userId, dto));
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.delete(id, userId));
    }


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PatchMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody CartUpdateDTO dto) {
        return Result.success(cartService.update(id, userId, dto));
    }


    /**
     * 执行 selectAll 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/select-all")
    public Result<Boolean> selectAll(@RequestHeader("X-User-Id") Long userId,
                                     @Valid @RequestBody CartSelectAllDTO dto) {
        return Result.success(cartService.selectAll(
            userId, dto.getIsSelected() ? 1 : 0));
    }


    /**
     * 执行 clear 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @DeleteMapping
    public Result<Boolean> clear(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.clear(userId));
    }


    /**
     * 执行 moveToFavorite 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/move-to-favorite")
    public Result<Void> moveToFavorite(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody CartMoveToFavoriteDTO dto) {
        cartService.moveToFavorite(userId, dto.getItemIds());
        return Result.success(null);
    }
}
