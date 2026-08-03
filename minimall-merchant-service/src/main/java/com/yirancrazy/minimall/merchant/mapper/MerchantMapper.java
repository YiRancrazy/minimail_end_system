package com.yirancrazy.minimall.merchant.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis Mapper 接口，提供数据库映射操作。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface MerchantMapper extends BaseMapper<MerchantPO> {
}
