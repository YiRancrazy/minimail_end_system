package com.yirancrazy.minimall.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.AddressCreateDto;
import com.yirancrazy.minimall.user.dto.AddressUpdateDto;
import com.yirancrazy.minimall.user.entity.AddressPo;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.service.AddressService;
import com.yirancrazy.minimall.user.vo.AddressVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址业务实现，负责每用户最多 20 条地址、归属校验、首条自动默认及默认地址事务切换.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

  private static final int MAX_ADDRESSES_PER_USER = 20;

  private final AddressManager addressManager;

  /**
   * 注入收货地址数据访问边界。
   *
   * @param addressManager 收货地址 Manager
   */
  public AddressServiceImpl(AddressManager addressManager) {
    this.addressManager = addressManager;
  }

  /**
   * 查询指定用户的收货地址，并按默认状态和创建时间倒序展示。
   *
   * @param userId 用户 ID
   * @return 地址视图列表；用户无地址时为空列表
   */
  @Override
  public List<AddressVo> listByUserId(Long userId) {
    return addressManager
        .list(
            Wrappers.lambdaQuery(AddressPo.class)
                .eq(AddressPo::getUserId, userId)
                .orderByDesc(AddressPo::getIsDefault)
                .orderByDesc(AddressPo::getCreateTime))
        .stream()
        .map(AddressVo::from)
        .toList();
  }

  /**
   * 新增收货地址，并在事务内执行数量限制和首条默认地址规则。
   *
   * <p>每个用户最多 20 条地址；当已有地址数为 0 时，新地址的 isDefault 写为 1，否则保持 NULL。
   *
   * @param userId 用户 ID
   * @param dto 地址创建参数
   * @return 新地址主键
   * @throws BizException 地址数量达到 20 条时抛出
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long create(Long userId, AddressCreateDto dto) {
    long count =
        addressManager.count(
            Wrappers.lambdaQuery(AddressPo.class).eq(AddressPo::getUserId, userId));
    if (count >= MAX_ADDRESSES_PER_USER) {
      throw new BizException(UserCodeEnum.ADDRESS_LIMIT_EXCEEDED);
    }

    AddressPo po = new AddressPo();
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
   * 修改当前用户拥有的地址，并在事务内先执行归属校验。
   *
   * @param userId 当前用户 ID
   * @param id 收货地址 ID
   * @param dto 地址更新参数
   * @return Manager 是否成功更新记录
   * @throws BizException 地址不存在或不属于当前用户时抛出
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean update(Long userId, Long id, AddressUpdateDto dto) {
    AddressPo existing = getOwnedAddress(userId, id);

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
   * 删除当前用户拥有的地址，并在事务内先校验地址归属。
   *
   * @param userId 当前用户 ID
   * @param id 收货地址 ID
   * @throws BizException 地址不存在或不属于当前用户时抛出
   */
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long userId, Long id) {
    getOwnedAddress(userId, id);
    addressManager.removeById(id);
    log.info("收货地址已删除，userId={}，addressId={}", userId, id);
  }

  /**
   * 在事务内切换当前用户的默认收货地址。
   *
   * <p>先清除该用户现有默认标记，再将目标地址设为默认；目标地址必须归属当前用户。
   *
   * @param userId 当前用户 ID
   * @param id 目标收货地址 ID
   * @throws BizException 地址不存在或不属于当前用户时抛出
   */
  @Transactional(rollbackFor = Exception.class)
  public void setDefault(Long userId, Long id) {
    AddressPo existing = getOwnedAddress(userId, id);

    // 将旧默认置空。非默认地址统一以 NULL 表示，配合唯一索引 (user_id,is_default,is_deleted)
    // 允许同一用户存在多条非默认地址，且保证默认地址最多一条（is_default=1）
    addressManager.update(
        Wrappers.lambdaUpdate(AddressPo.class)
            .eq(AddressPo::getUserId, userId)
            .eq(AddressPo::getIsDefault, 1)
            .set(AddressPo::getIsDefault, null));

    existing.setIsDefault(1);
    addressManager.updateById(existing);
    log.info("默认收货地址已设置，userId={}，addressId={}", userId, id);
  }

  /**
   * 查询地址并校验其归属当前用户。
   *
   * @param userId 当前用户 ID
   * @param id 收货地址 ID
   * @return 已通过归属校验的地址持久化对象
   * @throws BizException 地址不存在或不属于当前用户时抛出
   */
  private AddressPo getOwnedAddress(Long userId, Long id) {
    AddressPo po = addressManager.getById(id);
    if (po == null || !userId.equals(po.getUserId())) {
      throw new BizException(UserCodeEnum.ADDRESS_NOT_FOUND);
    }
    return po;
  }
}
