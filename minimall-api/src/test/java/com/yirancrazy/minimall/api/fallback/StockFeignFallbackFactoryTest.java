package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.api.feign.StockFeignClient;

/**
* StockFeignFallbackFactory 单元测试，验证服务不可用时返回的降级结果。
 */
public class StockFeignFallbackFactoryTest {

    @Test
    public void fallback_returns_false() {
        StockFeignFallbackFactory f = new StockFeignFallbackFactory();
        StockFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertFalse(client.reserve(null).getData());
    }
}