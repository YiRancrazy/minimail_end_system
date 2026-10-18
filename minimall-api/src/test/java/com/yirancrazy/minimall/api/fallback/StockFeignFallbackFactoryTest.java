package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockFeignFallbackFactoryTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class StockFeignFallbackFactoryTest {

    /**
     * 执行 fallback_returns_sys_error 方法。
     */
    public void fallback_returns_sys_error() {
        StockFeignFallbackFactory f = new StockFeignFallbackFactory();
        StockFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        Result<Boolean> resp = client.reserve(null);
        assertEquals(CommonCode.SYS_ERROR, resp.getCode());
        assertNull(resp.getData());
    }
}
