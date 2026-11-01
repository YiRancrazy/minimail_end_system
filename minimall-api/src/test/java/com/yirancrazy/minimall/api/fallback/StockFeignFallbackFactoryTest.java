package com.yirancrazy.minimall.api.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验库存服务降级预占返回系统错误且不携带成功数据.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class StockFeignFallbackFactoryTest {

    /** 库存服务不可达时预占应返回系统错误. */
    public void fallbackReturnsSysError() {
        StockFeignFallbackFactory f = new StockFeignFallbackFactory();
        StockFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);
        Result<Boolean> resp = client.reserve(null);
        assertEquals(CommonCode.SYS_ERROR, resp.getCode());
        assertNull(resp.getData());
    }
}
