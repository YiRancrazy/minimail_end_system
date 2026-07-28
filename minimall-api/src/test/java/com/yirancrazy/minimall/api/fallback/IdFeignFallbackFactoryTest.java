package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.IdFeignClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IdFeignFallbackFactoryTest {

    @Test
    void fallback_returnsMinusOneAndLogsWarning() {
        IdFeignFallbackFactory f = new IdFeignFallbackFactory();
        IdFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        assertEquals(-1L, client.nextId("order"));
    }
}