package com.yirancrazy.minimall.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.order.service.OrderService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderApplicationIntegrationTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    @MockBean
    private IdFeignClient idFeignClient;

    @Autowired
    private OrderService orderService;


    @Test
    void context_loads_and_order_service_bean_present() {
        assertNotNull(orderService, "OrderService bean should be loaded");
    }
}
