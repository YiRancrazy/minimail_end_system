package com.yirancrazy.minimall.goods.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品数据访问管理接口，继承 MP IService 复用通用 CRUD，供 Service 层调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface SkuManager extends IService<SkuPO> {
}