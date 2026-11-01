package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.user.dto.AddressCreateDto;
import com.yirancrazy.minimall.user.dto.AddressUpdateDto;
import com.yirancrazy.minimall.user.vo.AddressVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址业务接口，定义用户地址的查询、创建、修改、删除和默认地址切换能力，并约束地址归属校验.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface AddressService {

    /**
     * 查询指定用户的全部收货地址。
     *
     * <p>结果按默认地址优先、创建时间倒序排列；无地址时返回空列表。
     *
     * @param userId 用户 ID
     * @return 收货地址视图列表
     */
    List<AddressVo> listByUserId(Long userId);

    /**
     * 为指定用户新增收货地址。
     *
     * <p>单用户最多保存 20 条地址；首条地址自动设为默认地址，写入在新事务中完成。
     *
     * @param userId 用户 ID
     * @param dto 已通过请求校验的新增参数
     * @return 新地址 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址数量达到上限时抛出
     */
    Long create(Long userId, AddressCreateDto dto);

    /**
     * 修改指定用户拥有的收货地址。
     *
     * <p>更新前校验地址存在且归属当前用户，更新操作在事务中执行。
     *
     * @param userId 当前用户 ID
     * @param id 收货地址 ID
     * @param dto 更新后的地址字段
     * @return Manager 是否成功更新记录
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址不存在或不属于当前用户时抛出
     */
    boolean update(Long userId, Long id, AddressUpdateDto dto);

    /**
     * 删除指定用户拥有的收货地址。
     *
     * <p>删除前校验地址归属，删除操作在事务中执行。
     *
     * @param userId 当前用户 ID
     * @param id 收货地址 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址不存在或不属于当前用户时抛出
     */
    void delete(Long userId, Long id);

    /**
     * 将指定收货地址切换为当前用户的默认地址。
     *
     * <p>切换在事务中先清除该用户原默认标记，再设置目标地址，依赖数据库唯一约束限制默认地址最多一条。
     *
     * @param userId 当前用户 ID
     * @param id 收货地址 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 地址不存在或不属于当前用户时抛出
     */
    void setDefault(Long userId, Long id);
}
