package com.yirancrazy.minimall.user.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.AddressCreateDTO;
import com.yirancrazy.minimall.user.dto.AddressUpdateDTO;
import com.yirancrazy.minimall.user.entity.AddressPO;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.service.AddressService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址领域服务实现，实现收货地址增删改查与默认地址设置业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final AddressManager addressManager;

    public AddressServiceImpl(AddressManager addressManager) {
        this.addressManager = addressManager;
    }

    /**
     * 查询指定用户的全部收货地址列表，按是否默认和创建时间倒序排列。
     * @param userId 用户ID
     * @return 收货地址列表
     */
    @Override
    public List<AddressPO> listByUserId(Long userId) {
        return addressManager.list(Wrappers.lambdaQuery(AddressPO.class)
            .eq(AddressPO::getUserId, userId)
            .orderByDesc(AddressPO::getIsDefault)
            .orderByDesc(AddressPO::getCreateTime));
    }

    /**
     * 创建收货地址，每个用户最多 20 条；用户无地址时自动设为默认。
     * @param userId 用户ID
     * @param dto 创建入参
     * @return 新地址ID
     * @throws BizException 当地址数量达到上限时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, AddressCreateDTO dto) {
        long count = addressManager.count(Wrappers.lambdaQuery(AddressPO.class)
            .eq(AddressPO::getUserId, userId));
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BizException(UserCodeEnum.ADDRESS_LIMIT_EXCEEDED);
        }

        AddressPO po = new AddressPO();
        po.setUserId(userId);
        po.setReceiverName(dto.getReceiver());
        po.setReceiverPhone(dto.getPhone());
        po.setProvince(dto.getProvince());
        po.setCity(dto.getCity());
        po.setDistrict(dto.getDistrict());
        po.setDetailAddress(dto.getDetail());
        po.setIsDefault(count == 0 ? 1 : 0);

        addressManager.save(po);
        log.info("address created, userId={}, addressId={}", userId, po.getId());
        return po.getId();
    }

    /**
     * 更新收货地址，校验地址归属。
     * @param userId 用户ID
     * @param id 地址ID
     * @param dto 更新入参
     * @return 更新是否成功
     * @throws BizException 当地址不存在或不属于当前用户时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Long userId, Long id, AddressUpdateDTO dto) {
        AddressPO existing = getOwnedAddress(userId, id);

        existing.setReceiverName(dto.getReceiver());
        existing.setReceiverPhone(dto.getPhone());
        existing.setProvince(dto.getProvince());
        existing.setCity(dto.getCity());
        existing.setDistrict(dto.getDistrict());
        existing.setDetailAddress(dto.getDetail());

        boolean ok = addressManager.updateById(existing);
        log.info("address updated, userId={}, addressId={}", userId, id);
        return ok;
    }

    /**
     * 删除收货地址，校验地址归属。
     * @param userId 用户ID
     * @param id 地址ID
     * @throws BizException 当地址不存在或不属于当前用户时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedAddress(userId, id);
        addressManager.removeById(id);
        log.info("address deleted, userId={}, addressId={}", userId, id);
    }

    /**
     * 设置默认地址，事务内清除旧默认并设置新默认。
     * @param userId 用户ID
     * @param id 地址ID
     * @throws BizException 当地址不存在或不属于当前用户时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        AddressPO existing = getOwnedAddress(userId, id);

        addressManager.update(Wrappers.lambdaUpdate(AddressPO.class)
            .eq(AddressPO::getUserId, userId)
            .eq(AddressPO::getIsDefault, 1)
            .set(AddressPO::getIsDefault, 0));

        existing.setIsDefault(1);
        addressManager.updateById(existing);
        log.info("default address set, userId={}, addressId={}", userId, id);
    }

    private AddressPO getOwnedAddress(Long userId, Long id) {
        AddressPO po = addressManager.getById(id);
        if (po == null || !userId.equals(po.getUserId())) {
            throw new BizException(UserCodeEnum.ADDRESS_NOT_FOUND);
        }
        return po;
    }
}
