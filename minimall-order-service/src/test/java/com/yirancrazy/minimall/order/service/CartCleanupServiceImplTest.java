package com.yirancrazy.minimall.order.service;

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
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CartCleanupStatusEnum;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPo;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.service.impl.CartCleanupServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证购物车清理任务的错误分级、重试退避和批量扫描隔离。
 *   业务边界：使用 MyBatis-Plus 表元数据、Manager 桩、远程网关桩和内存 ObjectMapper。
 *   使用场景：覆盖瞬时失败、不可重试失败、非法 JSON 和坏任务不阻塞后续任务。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class CartCleanupServiceImplTest {

  private CartCleanupTaskManager taskManager;
  private OrderRemoteGateway orderRemoteGateway;
  private CartCleanupServiceImpl service;

  /**
   * 初始化购物车清理任务实体元数据。
   *
   * <p>在测试类加载前注册 CartCleanupTaskPo，保证 Lambda 条件更新可用。
   */
  @BeforeAll
  static void initLambdaCache() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""), CartCleanupTaskPo.class);
  }

  /**
   * 初始化当前测试类的共享夹具。
   *
   * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
   */
  @BeforeEach
  void setUp() {
    taskManager = mock(CartCleanupTaskManager.class);
    orderRemoteGateway = mock(OrderRemoteGateway.class);
    service = new CartCleanupServiceImpl(taskManager, orderRemoteGateway, new ObjectMapper());
    when(taskManager.update(any(Wrapper.class))).thenReturn(true);
  }

  /** 验证远端瞬时失败时清理任务保持待处理并增加重试. */
  @Test
  void testTryCleanupTransientFailureKeepsPending() {
    CartCleanupTaskPo task = pendingTask(1L, "[]");
    task.setItemsJson("[{\"cartItemId\":9001,\"skuId\":100,\"quantity\":2}]");
    when(taskManager.getById(1L)).thenReturn(task);
    when(orderRemoteGateway.cleanupCheckoutCart(any()))
        .thenReturn(Result.fail(CommonCode.SYS_ERROR, "cart down"));

    assertFalse(service.tryCleanup(1L));

    assertEquals(CartCleanupStatusEnum.PENDING.intCode(), task.getStatus());
    assertEquals(1, task.getRetryCount());
  }

  /** 验证SKU 不匹配时清理任务永久失败且不重试. */
  @Test
  void testTryCleanupSkuMismatchMarksFailedWithoutRetry() {
    CartCleanupTaskPo task = pendingTask(1L, "[]");
    task.setItemsJson("[{\"cartItemId\":9001,\"skuId\":100,\"quantity\":2}]");
    when(taskManager.getById(1L)).thenReturn(task);
    when(orderRemoteGateway.cleanupCheckoutCart(any()))
        .thenReturn(Result.fail("14004", "sku mismatch"));

    assertFalse(service.tryCleanup(1L));

    assertEquals(CartCleanupStatusEnum.FAILED.intCode(), task.getStatus());
  }

  /** 验证条目 JSON 损坏时清理任务永久失败且不重试. */
  @Test
  void testTryCleanupInvalidJsonMarksFailedWithoutRetry() {
    CartCleanupTaskPo task = pendingTask(1L, "not-json");
    when(taskManager.getById(1L)).thenReturn(task);

    assertFalse(service.tryCleanup(1L));

    assertEquals(CartCleanupStatusEnum.FAILED.intCode(), task.getStatus());
  }

  /** 验证损坏任务不会阻塞后续购物车清理任务. */
  @Test
  void testScanPendingBadTaskDoesNotBlockNextTask() {
    CartCleanupTaskPo bad = pendingTask(1L, "not-json");
    CartCleanupTaskPo good =
        pendingTask(2L, "[{\"cartItemId\":9001,\"skuId\":100,\"quantity\":2}]");
    when(taskManager.list(any(Wrapper.class))).thenReturn(List.of(bad, good));
    when(taskManager.getById(1L)).thenThrow(new IllegalStateException("db blip"));
    when(taskManager.getById(2L)).thenReturn(good);
    when(orderRemoteGateway.cleanupCheckoutCart(any())).thenReturn(Result.success());

    assertEquals(1, service.scanPending());
    assertEquals(CartCleanupStatusEnum.SUCCESS.intCode(), good.getStatus());
    verify(orderRemoteGateway).cleanupCheckoutCart(any());
  }

  /**
   * 构造已到期的购物车清理测试任务。
   *
   * <p>设置用户、条目 JSON、PENDING、零重试和过去时间的 nextRetryAt。
   *
   * @param id 记录或订单主键，具体实体由所属方法决定
   * @param itemsJson 购物车清理条目 JSON 快照
   * @return 可被扫描的清理任务。
   */
  private CartCleanupTaskPo pendingTask(Long id, String itemsJson) {
    CartCleanupTaskPo task = new CartCleanupTaskPo();
    task.setId(id);
    task.setUserId(7L);
    task.setItemsJson(itemsJson);
    task.setStatus(CartCleanupStatusEnum.PENDING.intCode());
    task.setRetryCount(0);
    task.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
    return task;
  }
}
