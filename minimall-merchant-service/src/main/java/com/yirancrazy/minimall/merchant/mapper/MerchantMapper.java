package com.yirancrazy.minimall.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质 MyBatis-Plus Mapper；负责 t_merch_merchant 与 MerchantPo 的基础 CRUD 映射，当前未声明自定义 SQL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface MerchantMapper extends BaseMapper<MerchantPo> {}
