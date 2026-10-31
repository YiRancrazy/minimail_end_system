package com.yirancrazy.minimall.order;

import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证订单服务 Spring 上下文可在 test profile 下启动。
 *   业务边界：通过 MockBean 隔离全部 Feign 客户端，使用 H2 测试配置检查组件装配，不执行真实中间件调用。
 *   使用场景：订单模块构建期的上下文冒烟测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderApplicationIntegrationTest {

  @MockBean private GoodsFeignClient goodsFeignClient;

  @MockBean private CartFeignClient cartFeignClient;

  @MockBean private CartCleanupFeignClient cartCleanupFeignClient;

  @MockBean private StockFeignClient stockFeignClient;

  @MockBean private PayFeignClient payFeignClient;

  @MockBean private IdFeignClient idFeignClient;

  @Autowired private OrderService orderService;
}
