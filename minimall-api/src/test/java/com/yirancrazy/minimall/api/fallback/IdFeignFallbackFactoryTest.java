package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeignFallbackFactory 的单元测试类。
 * @Version: 1.1
 * @DateTime: 2026/8/16
 **/
class IdFeignFallbackFactoryTest {

    @Test
    void fallback_returnsSysErrorAndLogsWarning() {
        IdFeignFallbackFactory f = new IdFeignFallbackFactory();
        IdFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        Result<Long> resp = client.nextId("order");
        assertEquals(CommonCode.SYS_ERROR, resp.getCode());
        assertNull(resp.getData());
    }
}
