package com.yirancrazy.minimall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.user.entity.AddressPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址 MyBatis-Plus Mapper，面向 t_user_address 提供 BaseMapper 通用 CRUD 映射.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface AddressMapper extends BaseMapper<AddressPo> {}
