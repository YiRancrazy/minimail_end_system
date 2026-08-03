package com.yirancrazy.minimall.pay.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis Mapper 接口，提供 t_merchant_withdraw 数据库映射操作。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface MerchantWithdrawMapper extends BaseMapper<MerchantWithdrawPO> {
}
