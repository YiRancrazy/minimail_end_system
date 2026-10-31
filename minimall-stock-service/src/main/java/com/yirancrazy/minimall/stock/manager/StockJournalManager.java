package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.entity.StockJournalPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装库存流水的保存、查询和分页访问，供服务层追踪库存变动。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface StockJournalManager extends IService<StockJournalPo> {}
