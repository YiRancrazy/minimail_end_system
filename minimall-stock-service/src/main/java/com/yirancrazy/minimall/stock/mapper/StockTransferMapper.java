package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransferMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Mapper
public interface StockTransferMapper extends BaseMapper<StockTransferPO> {
}
