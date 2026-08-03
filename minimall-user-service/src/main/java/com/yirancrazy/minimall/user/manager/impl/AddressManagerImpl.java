package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.AddressPO;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.mapper.AddressMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址数据访问层实现，封装 t_user_address 表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Manager
public class AddressManagerImpl extends ServiceImpl<AddressMapper, AddressPO>
    implements AddressManager {
}
