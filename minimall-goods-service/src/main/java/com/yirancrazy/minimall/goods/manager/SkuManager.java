package com.yirancrazy.minimall.goods.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
* 商品数据访问管理接口，继承 MP IService 复用通用 CRUD，供 Service 层调用。
 */
public interface SkuManager extends IService<SkuPO> {
}