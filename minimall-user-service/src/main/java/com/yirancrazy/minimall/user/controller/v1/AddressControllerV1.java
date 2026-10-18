package com.yirancrazy.minimall.user.controller.v1;

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
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.AddressCreateDTO;
import com.yirancrazy.minimall.user.dto.AddressUpdateDTO;
import com.yirancrazy.minimall.user.service.AddressService;
import com.yirancrazy.minimall.user.vo.AddressVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/addresses")
public class AddressControllerV1 {

    private final AddressService addressService;

    /**
     * 构造 AddressControllerV1 实例。
     * @param addressService addressService 参数
     */
    public AddressControllerV1(AddressService addressService) {
        this.addressService = addressService;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<List<AddressVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(addressService.listByUserId(userId));
    }


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody AddressCreateDTO dto) {
        return Result.success(addressService.create(userId, dto));
    }


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@RequestHeader("X-User-Id") Long userId,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody AddressUpdateDTO dto) {
        return Result.success(addressService.update(userId, id, dto));
    }


    /**
     * 删除业务数据。
     * @param userId userId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("X-User-Id") Long userId,
                               @PathVariable("id") Long id) {
        addressService.delete(userId, id);
        return Result.success(null);
    }


    /**
     * 设置相关数据。
     * @param userId userId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable("id") Long id) {
        addressService.setDefault(userId, id);
        return Result.success(null);
    }
}
