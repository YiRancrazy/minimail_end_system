package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.entity.StockPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存数据访问管理接口，继承 MyBatis-Plus IService 复用 StockPO 的通用 CRUD，
 *               为库存领域服务屏蔽 Mapper 细节，承担跨 Mapper 编排入口。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface StockManager extends IService<StockPO> {
}