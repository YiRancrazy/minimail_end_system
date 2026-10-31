package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.entity.StockTransferPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装库存调拨记录的持久化访问，供服务层保存和分页查询审计数据。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface StockTransferManager extends IService<StockTransferPo> {}
