package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.api.feign.IdFeignClient;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeignFallbackFactory 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class IdFeignFallbackFactoryTest {

    @Test
    void fallback_returnsMinusOneAndLogsWarning() {
        IdFeignFallbackFactory f = new IdFeignFallbackFactory();
        IdFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertEquals(-1L, client.nextId("order"));
    }
}