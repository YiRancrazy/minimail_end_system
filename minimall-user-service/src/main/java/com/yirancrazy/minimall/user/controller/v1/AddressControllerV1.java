package com.yirancrazy.minimall.user.controller.v1;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.AddressCreateDTO;
import com.yirancrazy.minimall.user.dto.AddressUpdateDTO;
import com.yirancrazy.minimall.user.entity.AddressPO;
import com.yirancrazy.minimall.user.service.AddressService;
import com.yirancrazy.minimall.user.vo.AddressVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址控制器，提供收货地址 CRUD 与默认地址设置 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressControllerV1 {

    private final AddressService addressService;

    public AddressControllerV1(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 查询当前用户的全部收货地址列表。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 收货地址列表
     */
    @GetMapping
    public Result<List<AddressVO>> list(@RequestHeader("X-User-Id") Long userId) {
        List<AddressPO> poList = addressService.listByUserId(userId);
        List<AddressVO> voList = poList.stream().map(AddressVO::from).toList();
        return Result.success(voList);
    }

    /**
     * 创建收货地址。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param dto 创建入参
     * @return 新地址ID
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody AddressCreateDTO dto) {
        return Result.success(addressService.create(userId, dto));
    }

    /**
     * 更新收货地址。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param id 地址ID
     * @param dto 更新入参
     * @return 更新是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@RequestHeader("X-User-Id") Long userId,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody AddressUpdateDTO dto) {
        return Result.success(addressService.update(userId, id, dto));
    }

    /**
     * 删除收货地址。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param id 地址ID
     * @return 无业务数据的成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId,
                               @PathVariable("id") Long id) {
        addressService.delete(userId, id);
        return Result.success(null);
    }

    /**
     * 设置默认收货地址。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param id 地址ID
     * @return 无业务数据的成功响应
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable("id") Long id) {
        addressService.setDefault(userId, id);
        return Result.success(null);
    }
}
