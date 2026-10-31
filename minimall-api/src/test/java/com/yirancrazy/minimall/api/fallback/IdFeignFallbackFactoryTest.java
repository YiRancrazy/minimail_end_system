package com.yirancrazy.minimall.api.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验 ID 服务降级返回系统错误且不暴露无效 ID.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class IdFeignFallbackFactoryTest {

  /** ID 服务不可达时应返回系统错误且数据为空. */
  @Test
  void testFallbackReturnsSysErrorAndLogsWarning() {
    IdFeignFallbackFactory f = new IdFeignFallbackFactory();
    IdFeignClient client = f.create(new RuntimeException("down"));
    assertNotNull(client);
    Result<Long> resp = client.nextId("order");
    assertEquals(CommonCode.SYS_ERROR, resp.getCode());
    assertNull(resp.getData());
  }
}
