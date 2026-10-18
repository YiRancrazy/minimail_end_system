package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.AddressPO;
import com.yirancrazy.minimall.user.manager.AddressManager;
import com.yirancrazy.minimall.user.mapper.AddressMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class AddressManagerImpl extends ServiceImpl<AddressMapper, AddressPO>
    implements AddressManager {
}
