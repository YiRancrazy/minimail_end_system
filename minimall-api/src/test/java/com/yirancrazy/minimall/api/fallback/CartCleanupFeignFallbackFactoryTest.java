package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理 Feign 降级测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class CartCleanupFeignFallbackFactoryTest {

    @Test
    void fallback_returnsRetryableSystemError() {
        CartCleanupFeignFallbackFactory factory = new CartCleanupFeignFallbackFactory();
        CartCleanupFeignClient client = factory.create(new RuntimeException("down"));
        CartCheckoutCleanupDTO dto = new CartCheckoutCleanupDTO(1L, java.util.List.of());

        assertEquals(CommonCode.SYS_ERROR, client.cleanupCheckout(dto).getCode());
    }
}
