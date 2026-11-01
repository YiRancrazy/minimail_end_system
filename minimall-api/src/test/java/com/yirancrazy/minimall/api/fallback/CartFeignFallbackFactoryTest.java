package com.yirancrazy.minimall.api.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验购物车降级计数返回 -1，结算快照返回系统错误.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class CartFeignFallbackFactoryTest {

    /** 计数应降级为 -1，结算快照应降级为系统错误. */
    @Test
    void testFallbackReturnsMinusOne() {
        CartFeignFallbackFactory f = new CartFeignFallbackFactory();
        CartFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertEquals(-1L, client.countByUser(1L).getData());
        assertEquals(
                "20000",
                client.getCheckoutItems(new CartCheckoutQueryDto(1L, java.util.List.of(9L))).getCode());
    }
}
