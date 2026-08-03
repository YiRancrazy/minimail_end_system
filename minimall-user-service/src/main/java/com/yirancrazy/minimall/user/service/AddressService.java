package com.yirancrazy.minimall.user.service;

import java.util.List;
import com.yirancrazy.minimall.user.dto.AddressCreateDTO;
import com.yirancrazy.minimall.user.dto.AddressUpdateDTO;
import com.yirancrazy.minimall.user.entity.AddressPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址领域服务接口，定义收货地址增删改查与默认地址设置业务契约
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public interface AddressService {

    /**
     * 查询指定用户的全部收货地址列表。
     * @param userId 用户ID
     * @return 收货地址列表
     */
    List<AddressPO> listByUserId(Long userId);

    /**
     * 创建收货地址，每个用户最多 20 条；用户无地址时自动设为默认。
     * @param userId 用户ID
     * @param dto 创建入参
     * @return 新地址ID
     */
    Long create(Long userId, AddressCreateDTO dto);

    /**
     * 更新收货地址，校验地址归属。
     * @param userId 用户ID
     * @param id 地址ID
     * @param dto 更新入参
     * @return 更新是否成功
     */
    boolean update(Long userId, Long id, AddressUpdateDTO dto);

    /**
     * 删除收货地址，校验地址归属。
     * @param userId 用户ID
     * @param id 地址ID
     */
    void delete(Long userId, Long id);

    /**
     * 设置默认地址，事务内清除旧默认并设置新默认。
     * @param userId 用户ID
     * @param id 地址ID
     */
    void setDefault(Long userId, Long id);
}
