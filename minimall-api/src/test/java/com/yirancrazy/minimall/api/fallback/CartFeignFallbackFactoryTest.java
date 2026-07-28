package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.CartFeignClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CartFeignFallbackFactoryTest {

    @Test
    void fallback_returnsMinusOne() {
        CartFeignFallbackFactory f = new CartFeignFallbackFactory();
        CartFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertEquals(-1L, client.countByUser(1L));
    }
}