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
import com.yirancrazy.minimall.user.vo.AddressVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final AddressManager addressManager;

    /**
     * 构造 AddressServiceImpl 实例。
     * @param addressManager addressManager 参数
     */
    public AddressServiceImpl(AddressManager addressManager) {
        this.addressManager = addressManager;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    @Override
    public List<AddressVO> listByUserId(Long userId) {
        return addressManager.list(Wrappers.lambdaQuery(AddressPO.class)
            .eq(AddressPO::getUserId, userId)
            .orderByDesc(AddressPO::getIsDefault)
            .orderByDesc(AddressPO::getCreateTime)).stream()
            .map(AddressVO::from)
            .toList();
    }


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 业务数值
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
        po.setIsDefault(count == 0 ? 1 : null);

        addressManager.save(po);
        log.info("收货地址已创建，userId={}，addressId={}", userId, po.getId());
        return po.getId();
    }


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
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
        log.info("收货地址已更新，userId={}，addressId={}", userId, id);
        return ok;
    }


    /**
     * 删除业务数据。
     * @param userId userId 参数
     * @param id id 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwnedAddress(userId, id);
        addressManager.removeById(id);
        log.info("收货地址已删除，userId={}，addressId={}", userId, id);
    }


    /**
     * 设置相关数据。
     * @param userId userId 参数
     * @param id id 参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        AddressPO existing = getOwnedAddress(userId, id);

        // 将旧默认置空。非默认地址统一以 NULL 表示，配合唯一索引 (user_id,is_default,is_deleted)
        // 允许同一用户存在多条非默认地址，且保证默认地址最多一条（is_default=1）
        addressManager.update(Wrappers.lambdaUpdate(AddressPO.class)
            .eq(AddressPO::getUserId, userId)
            .eq(AddressPO::getIsDefault, 1)
            .set(AddressPO::getIsDefault, null));

        existing.setIsDefault(1);
        addressManager.updateById(existing);
        log.info("默认收货地址已设置，userId={}，addressId={}", userId, id);
    }

    /**
     * 获取Owned地址。
     * @param userId 用户 ID
     * @param id ID
     * @return 查询结果
     * @throws BizException 业务校验失败时抛出
     */
    private AddressPO getOwnedAddress(Long userId, Long id) {
        AddressPO po = addressManager.getById(id);
        if (po == null || !userId.equals(po.getUserId())) {
            throw new BizException(UserCodeEnum.ADDRESS_NOT_FOUND);
        }
        return po;
    }
}
