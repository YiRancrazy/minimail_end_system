package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.AddressCreateDto;
import com.yirancrazy.minimall.user.dto.AddressUpdateDto;
import com.yirancrazy.minimall.user.service.AddressService;
import com.yirancrazy.minimall.user.vo.AddressVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收货地址 HTTP 入口，从可信请求头读取用户 ID，并将地址增删改查和默认地址切换请求转交 AddressService.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/addresses")
public class AddressControllerV1 {

    private final AddressService addressService;

    /**
     * 注入收货地址业务服务。
     *
     * @param addressService 收货地址服务
     */
    public AddressControllerV1(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 查询当前用户的全部收货地址。
     *
     * <p>用户身份取自网关注入的 X-User-Id 请求头。
     *
     * @param userId 当前用户 ID
     * @return 默认地址优先、创建时间倒序的地址列表
     */
    @GetMapping
    public Result<List<AddressVo>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(addressService.listByUserId(userId));
    }

    /**
     * 为当前用户新增收货地址。
     *
     * @param userId 当前用户 ID
     * @param dto 已通过校验的地址创建参数
     * @return 新地址 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址数量达到上限时抛出
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody AddressCreateDto dto) {
        return Result.success(addressService.create(userId, dto));
    }

    /**
     * 修改当前用户拥有的收货地址。
     *
     * @param userId 当前用户 ID
     * @param id 收货地址 ID
     * @param dto 已通过校验的地址更新参数
     * @return 是否成功更新
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址不存在或不属于当前用户时抛出
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id,
            @Valid @RequestBody AddressUpdateDto dto) {
        return Result.success(addressService.update(userId, id, dto));
    }

    /**
     * 删除当前用户拥有的收货地址。
     *
     * @param userId 当前用户 ID
     * @param id 收货地址 ID
     * @return data 为 null 的成功响应
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址不存在或不属于当前用户时抛出
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long id) {
        addressService.delete(userId, id);
        return Result.success(null);
    }

    /**
     * 将当前用户指定地址设为默认地址。
     *
     * @param userId 当前用户 ID
     * @param id 目标收货地址 ID
     * @return data 为 null 的成功响应
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址不存在或不属于当前用户时抛出
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(
            @RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long id) {
        addressService.setDefault(userId, id);
        return Result.success(null);
    }
}
