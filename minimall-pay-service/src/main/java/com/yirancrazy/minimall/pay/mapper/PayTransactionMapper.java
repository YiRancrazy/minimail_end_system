package com.yirancrazy.minimall.pay.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;

@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransactionPO> {
}