package com.yirancrazy.minimall.order.service;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CartCleanupStatusEnum;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPO;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.service.impl.CartCleanupServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理任务服务测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class CartCleanupServiceImplTest {

    private CartCleanupTaskManager taskManager;
    private OrderRemoteGateway orderRemoteGateway;
    private CartCleanupServiceImpl service;

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            CartCleanupTaskPO.class);
    }

    @BeforeEach
    void setUp() {
        taskManager = mock(CartCleanupTaskManager.class);
        orderRemoteGateway = mock(OrderRemoteGateway.class);
        service = new CartCleanupServiceImpl(
            taskManager, orderRemoteGateway, new ObjectMapper());
        when(taskManager.update(any(Wrapper.class))).thenReturn(true);
    }

    @Test
    void tryCleanup_transientFailure_keepsPending() {
        CartCleanupTaskPO task = pendingTask(1L, "[]");
        task.setItemsJson("[{\"cartItemId\":9001,\"skuId\":100,\"quantity\":2}]");
        when(taskManager.getById(1L)).thenReturn(task);
        when(orderRemoteGateway.cleanupCheckoutCart(any()))
            .thenReturn(Result.fail(CommonCode.SYS_ERROR, "cart down"));

        assertFalse(service.tryCleanup(1L));

        assertEquals(CartCleanupStatusEnum.PENDING.intCode(), task.getStatus());
        assertEquals(1, task.getRetryCount());
    }

    @Test
    void tryCleanup_skuMismatch_marksFailedWithoutRetry() {
        CartCleanupTaskPO task = pendingTask(1L, "[]");
        task.setItemsJson("[{\"cartItemId\":9001,\"skuId\":100,\"quantity\":2}]");
        when(taskManager.getById(1L)).thenReturn(task);
        when(orderRemoteGateway.cleanupCheckoutCart(any()))
            .thenReturn(Result.fail("14004", "sku mismatch"));

        assertFalse(service.tryCleanup(1L));

        assertEquals(CartCleanupStatusEnum.FAILED.intCode(), task.getStatus());
    }

    @Test
    void tryCleanup_invalidJson_marksFailedWithoutRetry() {
        CartCleanupTaskPO task = pendingTask(1L, "not-json");
        when(taskManager.getById(1L)).thenReturn(task);

        assertFalse(service.tryCleanup(1L));

        assertEquals(CartCleanupStatusEnum.FAILED.intCode(), task.getStatus());
    }

    @Test
    void scanPending_badTaskDoesNotBlockNextTask() {
        CartCleanupTaskPO bad = pendingTask(1L, "not-json");
        CartCleanupTaskPO good = pendingTask(2L,
            "[{\"cartItemId\":9001,\"skuId\":100,\"quantity\":2}]");
        when(taskManager.list(any(Wrapper.class))).thenReturn(List.of(bad, good));
        when(taskManager.getById(1L)).thenThrow(new IllegalStateException("db blip"));
        when(taskManager.getById(2L)).thenReturn(good);
        when(orderRemoteGateway.cleanupCheckoutCart(any()))
            .thenReturn(Result.success());

        assertEquals(1, service.scanPending());
        assertEquals(CartCleanupStatusEnum.SUCCESS.intCode(), good.getStatus());
        verify(orderRemoteGateway).cleanupCheckoutCart(any());
    }

    private CartCleanupTaskPO pendingTask(Long id, String itemsJson) {
        CartCleanupTaskPO task = new CartCleanupTaskPO();
        task.setId(id);
        task.setUserId(7L);
        task.setItemsJson(itemsJson);
        task.setStatus(CartCleanupStatusEnum.PENDING.intCode());
        task.setRetryCount(0);
        task.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        return task;
    }
}
