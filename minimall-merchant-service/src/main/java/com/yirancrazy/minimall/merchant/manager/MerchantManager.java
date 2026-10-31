package com.yirancrazy.minimall.merchant.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质 Manager 接口；继承 MyBatis-Plus IService，提供 MerchantPo 的基础持久化能力，不承载跨实体业务规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface MerchantManager extends IService<MerchantPo> {}
