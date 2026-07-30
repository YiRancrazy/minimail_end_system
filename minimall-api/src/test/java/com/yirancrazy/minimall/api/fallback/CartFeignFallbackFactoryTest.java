package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.api.feign.CartFeignClient;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartFeignFallbackFactory 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class CartFeignFallbackFactoryTest {

    @Test
    void fallback_returnsMinusOne() {
        CartFeignFallbackFactory f = new CartFeignFallbackFactory();
        CartFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertEquals(-1L, client.countByUser(1L));
    }
}