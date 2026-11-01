package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.AddressPo;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.mapper.AddressMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址 Manager 实现，将 AddressMapper 与 AddressPo 接入 MyBatis-Plus ServiceImpl 通用查询和持久化能力.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class AddressManagerImpl extends ServiceImpl<AddressMapper, AddressPo>
        implements AddressManager {}
