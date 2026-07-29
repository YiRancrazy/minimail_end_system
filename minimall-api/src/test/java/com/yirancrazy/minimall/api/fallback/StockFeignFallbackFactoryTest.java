package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.StockFeignClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockFeignFallbackFactory 单元测试，验证服务不可用时返回的降级结果。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public class StockFeignFallbackFactoryTest {

    @Test
    public void fallback_returns_false() {
        StockFeignFallbackFactory f = new StockFeignFallbackFactory();
        StockFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertFalse(client.reserve(null));
    }
}