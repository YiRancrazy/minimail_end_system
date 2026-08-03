package com.yirancrazy.minimall.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.user.entity.AddressPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址 MyBatis Mapper 接口，映射 t_user_address 表
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface AddressMapper extends BaseMapper<AddressPO> {
}
