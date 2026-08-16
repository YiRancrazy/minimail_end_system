package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
* StockFeignFallbackFactory 单元测试，验证服务不可用时返回系统错误而非伪造成功。
 */
public class StockFeignFallbackFactoryTest {

    @Test
    public void fallback_returns_sys_error() {
        StockFeignFallbackFactory f = new StockFeignFallbackFactory();
        StockFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        Result<Boolean> resp = client.reserve(null);
        assertEquals(CommonCode.SYS_ERROR, resp.getCode());
        assertNull(resp.getData());
    }
}
