package com.yirancrazy.minimall.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务集成测试，验证 Spring 上下文启动与核心 Bean 装配，FeignClient 用 MockBean 隔离外部依赖。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@SpringBootTest
@ActiveProfiles("test")
class OrderApplicationIntegrationTest {

    @MockBean
    private GoodsFeignClient goodsFeignClient;

    @MockBean
    private StockFeignClient stockFeignClient;

    @MockBean
    private PayFeignClient payFeignClient;

    @Autowired
    private OrderService orderService;

    /**
     * 验证 Spring 上下文启动成功且 OrderService Bean 装配完成。
     */
    @Test
    void context_loads_and_order_service_bean_present() {
        assertNotNull(orderService, "OrderService bean should be loaded");
    }
}
