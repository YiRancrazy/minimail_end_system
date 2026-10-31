package com.yirancrazy.minimall.api.config;

import com.yirancrazy.minimall.api.fallback.CartCleanupFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.CartFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.GoodsFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.MerchantFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.NotifyFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.PayFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.StockFeignFallbackFactory;
import com.yirancrazy.minimall.api.fallback.UserFeignFallbackFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 注册全部 Feign fallback 工厂，供业务服务统一使用.
 * @Version: 1.0
 * @DateTime: 2026/10/21
 */
@AutoConfiguration
@Import({
  CartCleanupFeignFallbackFactory.class,
  CartFeignFallbackFactory.class,
  GoodsFeignFallbackFactory.class,
  IdFeignFallbackFactory.class,
  MerchantFeignFallbackFactory.class,
  NotifyFeignFallbackFactory.class,
  OrderFeignFallbackFactory.class,
  PayFeignFallbackFactory.class,
  StockFeignFallbackFactory.class,
  UserFeignFallbackFactory.class
})
public class FeignFallbackAutoConfiguration {}
