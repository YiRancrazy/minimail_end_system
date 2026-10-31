package com.yirancrazy.minimall.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.cart.dto.CartItemAddDto;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDto;
import com.yirancrazy.minimall.cart.dto.CartUpdateDto;
import com.yirancrazy.minimall.cart.entity.CartItemPo;
import com.yirancrazy.minimall.cart.gateway.CartRemoteGateway;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.cart.service.impl.CartServiceImpl;
import com.yirancrazy.minimall.cart.vo.CartItemVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mockito 协作者以及 MySQL 模式 H2 数据库定义 CartServiceImpl 行为场景，覆盖
 *     Mapper upsert、唯一键和数量上限检查。测试不启动 Spring；JUnit 只发现带 @Test 的方法，
 *     未带该注解的存量公开场景方法仍保留，但不会由 JUnit 5 执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class CartServiceImplTest {

  private static DataSource dataSource;
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void initLambdaCache() throws Exception {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""), CartItemPo.class);
    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
    driverManagerDataSource.setDriverClassName("org.h2.Driver");
    driverManagerDataSource.setUrl(
        "jdbc:h2:mem:cart_t13;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    driverManagerDataSource.setUsername("sa");
    dataSource = driverManagerDataSource;
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE TABLE t_cart_item (
              id BIGINT PRIMARY KEY,
              user_id BIGINT NOT NULL,
              sku_id BIGINT NOT NULL,
              quantity INT,
              selected INT,
              version INT DEFAULT 0,
              is_deleted TINYINT DEFAULT 0,
              create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              UNIQUE (user_id, sku_id, is_deleted)
          )
          """);
    }
    MybatisConfiguration configuration = new MybatisConfiguration();
    configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
    configuration.addMapper(CartItemMapper.class);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  private CartItemManager cartItemManager;
  private CartItemMapper cartItemMapper;
  private CartRemoteGateway cartRemoteGateway;
  private MinioUtil minioUtil;
  private CartServiceImpl service;

  @BeforeEach
  void setUp() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DELETE FROM t_cart_item");
    }
    cartItemManager = mock(CartItemManager.class);
    cartItemMapper = mock(CartItemMapper.class);
    cartRemoteGateway = mock(CartRemoteGateway.class);
    minioUtil = mock(MinioUtil.class);
    lenient()
        .doAnswer(
            inv -> {
              CartItemPo p = inv.getArgument(0);
              if (p.getId() == null) {
                p.setId(System.nanoTime());
              }
              return true;
            })
        .when(cartItemManager)
        .save(any(CartItemPo.class));
    service = new CartServiceImpl(cartItemManager, cartRemoteGateway, minioUtil);
  }

  /**
   * 验证购物车查询会用完整 SKU 和 SPU 快照补全一条持久化记录.
   *
   * <p>前置条件：管理器返回一条已勾选记录，两个批量网关返回匹配快照。预期结果：视图包含 SKU、
   * 标题、规格和数量，且每个批量快照方法按预期主键调用一次。
   */
  public void listByUserReturnsItems() {
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    item.setSkuId(100L);
    item.setQuantity(2);
    item.setSelected(1);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
    when(cartRemoteGateway.batchSkuSnapshot(List.of(100L)))
        .thenReturn(
            Result.success(
                Map.of(
                    100L,
                    new SkuSnapshotDto(
                        100L, 10L, "S 白色", new java.math.BigDecimal("9.90"), 99, 7L))));
    when(cartRemoteGateway.batchSpuSnapshot(List.of(10L)))
        .thenReturn(
            Result.success(Map.of(10L, new SpuSnapshotDto(10L, "商品标题", "http://img/a.jpg"))));

    List<CartItemVo> result = service.listByUser(7L);

    assertEquals(1, result.size());
    CartItemVo vo = result.get(0);
    assertEquals(100L, vo.getSkuId());
    assertEquals("商品标题", vo.getTitle());
    assertEquals("S 白色", vo.getSkuSpec());
    verify(cartRemoteGateway).batchSkuSnapshot(List.of(100L));
    verify(cartRemoteGateway).batchSpuSnapshot(List.of(10L));
  }

  /**
   * 验证空购物车会立即返回，且不调用远程快照.
   *
   * <p>前置条件：管理器为用户 999 返回空列表。预期结果：查询结果为空，SKU 和 SPU 网关都不会被调用。
   */
  public void listByUserReturnsEmptyWhenNoItems() {
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

    List<CartItemVo> result = service.listByUser(999L);

    assertTrue(result.isEmpty());
    verify(cartRemoteGateway, never()).batchSkuSnapshot(any());
    verify(cartRemoteGateway, never()).batchSpuSnapshot(any());
  }

  /**
   * 验证 SKU 快照缺失时仍返回持久化购物车记录的部分视图.
   *
   * <p>前置条件：存在一条记录，但批量 SKU 查询返回空映射。预期结果：记录仍在结果中，价格和标题为
   * null，并跳过 SPU 查询。
   */
  public void listByUserHandlesNullSnapshot() {
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    item.setSkuId(100L);
    item.setQuantity(2);
    item.setSelected(1);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
    when(cartRemoteGateway.batchSkuSnapshot(any()))
        .thenReturn(Result.success(Collections.emptyMap()));

    List<CartItemVo> result = service.listByUser(7L);

    assertEquals(1, result.size());
    assertNull(result.get(0).getPrice());
    assertNull(result.get(0).getTitle());
    verify(cartRemoteGateway, never()).batchSpuSnapshot(any());
  }

  /**
   * 验证多条购物车记录通过一次 SKU 和一次 SPU 批量调用完成补全.
   *
   * <p>前置条件：两条记录解析为两个 SKU，且两个 SKU 共享一个 SPU。预期结果：返回两个视图，
   * 每个批量网关各调用一次。
   */
  public void listByUserBatchSnapshotCalledOnceForMultipleItems() {
    CartItemPo item1 = new CartItemPo();
    item1.setId(1L);
    item1.setUserId(7L);
    item1.setSkuId(100L);
    item1.setQuantity(1);
    item1.setSelected(1);
    CartItemPo item2 = new CartItemPo();
    item2.setId(2L);
    item2.setUserId(7L);
    item2.setSkuId(200L);
    item2.setQuantity(2);
    item2.setSelected(1);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item1, item2));
    when(cartRemoteGateway.batchSkuSnapshot(any()))
        .thenReturn(
            Result.success(
                Map.of(
                    100L,
                        new SkuSnapshotDto(
                            100L, 10L, "S 白色", new java.math.BigDecimal("9.90"), 99, 7L),
                    200L,
                        new SkuSnapshotDto(
                            200L, 10L, "L 黑色", new java.math.BigDecimal("19.90"), 50, 7L))));
    when(cartRemoteGateway.batchSpuSnapshot(any()))
        .thenReturn(
            Result.success(Map.of(10L, new SpuSnapshotDto(10L, "商品标题", "http://img/a.jpg"))));

    List<CartItemVo> result = service.listByUser(7L);

    assertEquals(2, result.size());
    verify(cartRemoteGateway).batchSkuSnapshot(any());
    verify(cartRemoteGateway).batchSpuSnapshot(any());
  }

  /**
   * 验证添加操作会把空勾选值默认为 1，并且只使用原子 upsert 路径.
   *
   * <p>前置条件：请求省略 selected，upsert 影响一行，持久化记录主键为 9。预期结果：add 返回 9，
   * 传入勾选值 1，且不调用 save 或 updateById。
   */
  public void addUsesAtomicUpsertAndDefaultsSelected() {
    CartItemPo persisted = cartItem(9L, 7L, 100L, 2, 1);
    when(cartItemManager.upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(1))).thenReturn(1);
    when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(persisted);
    CartItemAddDto dto = new CartItemAddDto();
    dto.setSkuId(100L);
    dto.setQuantity(2);
    dto.setSelected(null);

    Long id = service.add(7L, dto);

    assertEquals(9L, id);
    verify(cartItemManager).upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(1));
    verify(cartItemManager, never()).updateById(any(CartItemPo.class));
    verify(cartItemManager, never()).save(any(CartItemPo.class));
  }

  /**
   * 验证添加操作会保留显式传入的未勾选值.
   *
   * <p>前置条件：dto.selected 为 0，且 upsert 成功。预期结果：管理器收到勾选值 0，并返回持久化
   * 记录主键。
   */
  public void addPassesThroughSelectedWhenSet() {
    CartItemPo persisted = cartItem(9L, 7L, 100L, 2, 0);
    when(cartItemManager.upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(0))).thenReturn(1);
    when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(persisted);
    CartItemAddDto dto = new CartItemAddDto();
    dto.setSkuId(100L);
    dto.setQuantity(2);
    dto.setSelected(0);

    Long id = service.add(7L, dto);

    assertEquals(9L, id);
    verify(cartItemManager).upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(0));
  }

  /**
   * 验证删除用户拥有的记录时会返回管理器成功删除结果.
   *
   * <p>前置条件：记录 1 属于用户 7，removeById 返回 true。预期结果：delete 返回 true，并以记录 1
   * 调用 removeById。
   */
  public void deleteReturnsTrueOnSuccess() {
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    when(cartItemManager.getById(1L)).thenReturn(item);
    when(cartItemManager.removeById(1L)).thenReturn(true);
    boolean ok = service.delete(1L, 7L);
    assertTrue(ok);
    verify(cartItemManager).removeById(eq(1L));
  }

  /**
   * 验证归属校验通过后，删除操作会原样返回数据层的 false 结果.
   *
   * <p>前置条件：记录 1 属于用户 7，但 removeById 返回 false。预期结果：服务返回 false，
   * 不把数据层结果转换为业务异常。
   */
  public void deleteReturnsFalseOnFailure() {
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    when(cartItemManager.getById(1L)).thenReturn(item);
    when(cartItemManager.removeById(1L)).thenReturn(false);
    boolean ok = service.delete(1L, 7L);
    assertFalse(ok);
  }

  /**
   * 验证删除用户不匹配的记录时会在移除前拒绝请求.
   *
   * <p>前置条件：记录 1 属于用户 7，调用用户为 999。预期结果：抛出 BizException，且不调用
   * removeById。
   */
  public void deleteThrowsWhenUserMismatch() {
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    when(cartItemManager.getById(1L)).thenReturn(item);
    assertThrows(BizException.class, () -> service.delete(1L, 999L));
    verify(cartItemManager, never()).removeById(any());
  }

  /**
   * 验证非空购物车会返回数据层统计数量.
   *
   * <p>前置条件：管理器计数查询返回 5。预期结果：服务返回 5。
   */
  public void countByUserReturnsCount() {
    when(cartItemManager.count(any())).thenReturn(5L);

    long count = service.countByUser(7L);
    assertEquals(5L, count);
  }

  /**
   * 验证空购物车的统计结果保持为 0.
   *
   * <p>前置条件：用户 999 的管理器计数查询返回 0。预期结果：服务返回 0。
   */
  public void countByUserReturnsZeroWhenEmpty() {
    when(cartItemManager.count(any())).thenReturn(0L);
    long count = service.countByUser(999L);
    assertEquals(0L, count);
  }

  /**
   * 验证更新操作会拒绝不存在的购物车记录.
   *
   * <p>前置条件：记录 999 的 getById 返回 null。预期结果：在任何更新尝试前抛出 BizException。
   */
  public void updateThrowsWhenMissing() {
    when(cartItemManager.getById(999L)).thenReturn(null);
    CartUpdateDto dto = new CartUpdateDto();
    dto.setQuantity(3);
    assertThrows(BizException.class, () -> service.update(999L, 7L, dto));
  }

  /**
   * 验证更新操作会拒绝属于其他用户的购物车记录.
   *
   * <p>前置条件：记录 1 属于用户 7，调用用户为 999。预期结果：抛出 BizException，且不调用
   * updateById。
   */
  public void updateThrowsWhenUserMismatch() {
    CartItemPo existing = new CartItemPo();
    existing.setId(1L);
    existing.setUserId(7L);
    when(cartItemManager.getById(1L)).thenReturn(existing);
    CartUpdateDto dto = new CartUpdateDto();
    dto.setQuantity(3);
    assertThrows(BizException.class, () -> service.update(1L, 999L, dto));
    verify(cartItemManager, never()).updateById(any());
  }

  /**
   * 验证仅更新数量时会保留现有勾选编码.
   *
   * <p>前置条件：所属记录初始数量为 2、勾选值为 0，请求将数量改为 5。预期结果：update 返回 true，
   * 数量变为 5，勾选值仍为 0。
   */
  public void updateQuantityOnlyReturnsTrueOnSuccess() {
    CartItemPo existing = new CartItemPo();
    existing.setId(1L);
    existing.setUserId(7L);
    existing.setQuantity(2);
    existing.setSelected(0);
    when(cartItemManager.getById(1L)).thenReturn(existing);
    when(cartItemManager.updateById(any(CartItemPo.class))).thenReturn(true);

    CartUpdateDto dto = new CartUpdateDto();
    dto.setQuantity(5);
    boolean ok = service.update(1L, 7L, dto);

    assertTrue(ok);
    assertEquals(5, existing.getQuantity());
    assertEquals(0, existing.getSelected());
  }

  /**
   * 验证仅更新勾选状态时会保留现有数量.
   *
   * <p>前置条件：所属记录初始数量为 3、勾选值为 0，请求将其设为已勾选。预期结果：update 返回
   * true，勾选值变为 1，数量仍为 3。
   */
  public void updateSelectedOnlyReturnsTrueOnSuccess() {
    CartItemPo existing = new CartItemPo();
    existing.setId(1L);
    existing.setUserId(7L);
    existing.setQuantity(3);
    existing.setSelected(0);
    when(cartItemManager.getById(1L)).thenReturn(existing);
    when(cartItemManager.updateById(any(CartItemPo.class))).thenReturn(true);

    CartUpdateDto dto = new CartUpdateDto();
    dto.setIsSelected(true);
    boolean ok = service.update(1L, 7L, dto);

    assertTrue(ok);
    assertEquals(1, existing.getSelected());
    assertEquals(3, existing.getQuantity());
  }

  /**
   * 验证同时包含数量和勾选状态的请求会应用两个字段.
   *
   * <p>前置条件：所属记录初始数量为 2、勾选值为 0。预期结果：update 返回 true，数量变为 5，
   * 勾选值变为 1。
   */
  public void updateBothFieldsReturnsTrueOnSuccess() {
    CartItemPo existing = new CartItemPo();
    existing.setId(1L);
    existing.setUserId(7L);
    existing.setQuantity(2);
    existing.setSelected(0);
    when(cartItemManager.getById(1L)).thenReturn(existing);
    when(cartItemManager.updateById(any(CartItemPo.class))).thenReturn(true);

    CartUpdateDto dto = new CartUpdateDto();
    dto.setQuantity(5);
    dto.setIsSelected(true);
    boolean ok = service.update(1L, 7L, dto);

    assertTrue(ok);
    assertEquals(5, existing.getQuantity());
    assertEquals(1, existing.getSelected());
  }

  /**
   * 验证空更新请求不会改变已持久化字段.
   *
   * <p>前置条件：dto 的两个字段均为 null，updateById 返回 true。预期结果：服务仍报告成功，
   * 数量与勾选值保持原值。
   */
  public void updateNoFieldsChangedReturnsTrueOnSuccess() {
    CartItemPo existing = new CartItemPo();
    existing.setId(1L);
    existing.setUserId(7L);
    existing.setQuantity(2);
    existing.setSelected(0);
    when(cartItemManager.getById(1L)).thenReturn(existing);
    when(cartItemManager.updateById(any(CartItemPo.class))).thenReturn(true);

    CartUpdateDto dto = new CartUpdateDto();
    boolean ok = service.update(1L, 7L, dto);

    assertTrue(ok);
    assertEquals(2, existing.getQuantity());
    assertEquals(0, existing.getSelected());
  }

  /**
   * 验证添加已有 SKU 会返回合并后的记录，而不是创建新记录.
   *
   * <p>前置条件：upsert 影响两行，读取返回已有记录主键 1。预期结果：请求数量 3 传入 upsert，
   * 返回主键 1，且不调用 save 或 updateById。
   */
  public void addDelegatesMergeToAtomicUpsert() {
    CartItemPo persisted = cartItem(1L, 7L, 100L, 5, 1);
    when(cartItemManager.upsertQuantity(anyLong(), eq(7L), eq(100L), eq(3), eq(1))).thenReturn(2);
    when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(persisted);
    CartItemAddDto dto = new CartItemAddDto();
    dto.setSkuId(100L);
    dto.setQuantity(3);

    Long id = service.add(7L, dto);

    assertEquals(1L, id);
    verify(cartItemManager).upsertQuantity(anyLong(), eq(7L), eq(100L), eq(3), eq(1));
    verify(cartItemManager, never()).updateById(any(CartItemPo.class));
    verify(cartItemManager, never()).save(any(CartItemPo.class));
  }

  /**
   * 验证原子 upsert 未影响记录时添加操作返回系统错误.
   *
   * <p>前置条件：upsertQuantity 返回 0。预期结果：BizException 携带 CommonCode.SYS_ERROR，
   * 服务不会继续读取不存在的记录。
   */
  public void addThrowsWhenUpsertAffectsNoRows() {
    when(cartItemManager.upsertQuantity(anyLong(), anyLong(), anyLong(), anyInt(), anyInt()))
        .thenReturn(0);
    CartItemAddDto dto = new CartItemAddDto();
    dto.setSkuId(100L);
    dto.setQuantity(1);
    dto.setSelected(1);

    BizException exception = assertThrows(BizException.class, () -> service.add(7L, dto));

    assertEquals(CommonCode.SYS_ERROR, exception.getCode());
    verify(cartItemManager, never()).getByUserIdAndSkuId(anyLong(), anyLong());
  }

  /**
   * 验证 upsert 成功但无法回读记录时添加操作失败.
   *
   * <p>前置条件：upsertQuantity 返回 1，但 getByUserIdAndSkuId 返回 null。预期结果：
   * BizException 携带 CommonCode.SYS_ERROR。
   */
  public void addThrowsWhenUpsertedRowIsNotReadable() {
    when(cartItemManager.upsertQuantity(anyLong(), anyLong(), anyLong(), anyInt(), anyInt()))
        .thenReturn(1);
    when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(null);
    CartItemAddDto dto = new CartItemAddDto();
    dto.setSkuId(100L);
    dto.setQuantity(1);
    dto.setSelected(1);

    BizException exception = assertThrows(BizException.class, () -> service.add(7L, dto));

    assertEquals(CommonCode.SYS_ERROR, exception.getCode());
  }

  /**
   * 验证并发首次插入会收敛为一条记录并累加两次数量.
   *
   * <p>前置条件：两个线程同时启动，并针对用户 7、SKU 100 使用不同主键执行 upsert。预期结果：
   * H2 表中只有一条唯一记录，数量为 2。
   *
   * @throws Exception 线程协调、SQL 执行或等待超时失败时抛出
   */
  public void concurrentFirstInsertProducesSingleRowAndAccumulatesQuantity()
      throws Exception {
    ExecutorService executor =
        Executors.newFixedThreadPool(2, runnable -> new Thread(runnable, "cart-upsert-test"));
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<Integer> first = executor.submit(() -> runConcurrentUpsert(start, 1001L, 1));
      Future<Integer> second = executor.submit(() -> runConcurrentUpsert(start, 1002L, 1));
      start.countDown();
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    assertEquals(
        1, queryLong("SELECT COUNT(*) FROM t_cart_item WHERE user_id = 7 AND sku_id = 100"));
    assertEquals(
        2, queryInt("SELECT quantity FROM t_cart_item WHERE user_id = 7 AND sku_id = 100"));
  }

  /**
   * 验证原子 upsert 会将累加后的数量限制为 999.
   *
   * <p>前置条件：同一用户和 SKU 记录先累加 998，再累加 5。预期结果：保存数量为 999，
   * 而不是 1003。
   */
  public void upsertCapsQuantityAt999() {
    upsert(1001L, 998);
    upsert(1002L, 5);

    assertEquals(
        999, queryInt("SELECT quantity FROM t_cart_item WHERE user_id = 7 AND sku_id = 100"));
  }

  /**
   * 验证全选操作只委托一次批量勾选更新，并返回其 Boolean 结果.
   *
   * <p>前置条件：全选请求对应管理器更新返回 true。预期结果：服务返回 true，并调用一次基于
   * wrapper 的更新。
   */
  public void selectAllReturnsTrueOnSuccess() {
    when(cartItemManager.update(any(Wrapper.class))).thenReturn(true);

    CartSelectAllDto dto = new CartSelectAllDto();
    dto.setIsSelected(true);
    boolean ok = service.selectAll(7L, dto.getIsSelected() ? 1 : 0);

    assertTrue(ok);
    verify(cartItemManager).update(any(Wrapper.class));
  }

  /**
   * 验证清空操作只委托一次按用户范围的批量删除，并返回其结果.
   *
   * <p>前置条件：用户 7 的管理器删除操作返回 true。预期结果：服务返回 true，并调用一次基于
   * wrapper 的删除。
   */
  public void clearReturnsTrueOnSuccess() {
    when(cartItemManager.remove(any(Wrapper.class))).thenReturn(true);

    boolean ok = service.clear(7L);

    assertTrue(ok);
    verify(cartItemManager).remove(any(Wrapper.class));
  }

  /**
   * 验证收藏写入成功后会继续删除本地购物车记录.
   *
   * <p>前置条件：记录 1 解析为 SKU 100，用户服务返回成功。预期结果：网关收到用户 7 和 SKU 100，
   * 随后以记录 1 调用 removeByIds。
   */
  public void moveToFavoriteSuccessDeletesCartItem() {
    when(cartRemoteGateway.addFavorite(any(), any())).thenReturn(Result.success(null));
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    item.setSkuId(100L);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
    when(cartItemManager.removeByIds(List.of(1L))).thenReturn(true);

    service.moveToFavorite(7L, List.of(1L));

    verify(cartRemoteGateway).addFavorite(eq(7L), eq(100L));
    verify(cartItemManager).removeByIds(List.of(1L));
  }

  /**
   * 验证收藏写入失败时不会删除本地购物车记录.
   *
   * <p>前置条件：记录存在，但用户服务返回系统错误 Result。预期结果：抛出 BizException，
   * 且不调用 removeByIds。
   */
  public void moveToFavoriteFeignFailThrows() {
    when(cartRemoteGateway.addFavorite(any(), any()))
        .thenReturn(Result.fail(CommonCode.SYS_ERROR, "down"));
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    item.setSkuId(100L);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));

    assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L)));
    verify(cartItemManager, never()).removeByIds(any());
  }

  /**
   * 验证前一个收藏远程成功、后一个失败时仍保留全部本地记录.
   *
   * <p>前置条件：请求两个所属记录，SKU 100 收藏成功，SKU 200 收藏失败。预期结果：抛出
   * BizException 且不调用 removeByIds；该测试不会实际执行名称中隐含的重试。
   */
  public void moveToFavoritePartialFailureKeepsCartAndAllowsIdempotentRetry() {
    CartItemPo first = new CartItemPo();
    first.setId(1L);
    first.setUserId(7L);
    first.setSkuId(100L);
    CartItemPo second = new CartItemPo();
    second.setId(2L);
    second.setUserId(7L);
    second.setSkuId(200L);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(first, second));
    when(cartRemoteGateway.addFavorite(7L, 100L)).thenReturn(Result.success(null));
    when(cartRemoteGateway.addFavorite(7L, 200L))
        .thenReturn(Result.fail(CommonCode.SYS_ERROR, "down"));

    assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L, 2L)));

    verify(cartItemManager, never()).removeByIds(any());
  }

  /**
   * 验证远程收藏成功但本地删除失败时会抛出业务异常.
   *
   * <p>前置条件：收藏创建成功，但 removeByIds 返回 false。预期结果：远程调用发生后抛出
   * BizException。
   */
  public void moveToFavoriteDeleteFailThrows() {
    when(cartRemoteGateway.addFavorite(any(), any())).thenReturn(Result.success(null));
    CartItemPo item = new CartItemPo();
    item.setId(1L);
    item.setUserId(7L);
    item.setSkuId(100L);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
    when(cartItemManager.removeByIds(List.of(1L))).thenReturn(false);

    assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L)));
    verify(cartRemoteGateway).addFavorite(eq(7L), eq(100L));
  }

  /**
   * 验证移入收藏请求找不到购物车记录时会拒绝处理.
   *
   * <p>前置条件：请求主键 1 的归属查询返回空。预期结果：在任何收藏或删除调用前抛出 BizException。
   */
  public void moveToFavoriteMissingItemThrows() {
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

    assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L)));
    verify(cartRemoteGateway, never()).addFavorite(any(), any());
    verify(cartItemManager, never()).removeByIds(any());
  }

  /**
   * 验证移入收藏会在访问数据层前拒绝空条目主键列表.
   *
   * <p>前置条件：请求列表为空。预期结果：抛出 BizException，且不调用管理器列表方法。
   */
  public void moveToFavoriteEmptyItemIdsThrows() {
    assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of()));
    verify(cartItemManager, never()).list(any(Wrapper.class));
  }

  /**
   * 验证多个所属记录会按 SKU 逐个收藏，并执行一次本地批量删除.
   *
   * <p>前置条件：记录 1、2 分别解析为 SKU 100、200，收藏调用全部成功，removeByIds 返回 true。
   * 预期结果：两个 SKU 收藏调用均发生，删除接收列表 [1, 2]。
   */
  public void moveToFavoriteMultipleItemsFavoritesEachSkuAndDeletesByIds() {
    when(cartRemoteGateway.addFavorite(any(), any())).thenReturn(Result.success(null));
    CartItemPo item1 = new CartItemPo();
    item1.setId(1L);
    item1.setUserId(7L);
    item1.setSkuId(100L);
    CartItemPo item2 = new CartItemPo();
    item2.setId(2L);
    item2.setUserId(7L);
    item2.setSkuId(200L);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item1, item2));
    when(cartItemManager.removeByIds(List.of(1L, 2L))).thenReturn(true);

    service.moveToFavorite(7L, List.of(1L, 2L));

    verify(cartRemoteGateway).addFavorite(eq(7L), eq(100L));
    verify(cartRemoteGateway).addFavorite(eq(7L), eq(200L));
    verify(cartItemManager).removeByIds(List.of(1L, 2L));
  }

  /** 验证结算快照拒绝请求中存在但数据源未返回的缺失条目. */
  @Test
  void testGetCheckoutItemsRejectsMissingItem() {
    when(cartItemManager.list(any(Wrapper.class)))
        .thenReturn(List.of(cartItem(1L, 7L, 100L, 2, 1)));

    assertThrows(BizException.class, () -> service.getCheckoutItems(7L, List.of(1L, 2L)));
  }

  /** 验证结算快照拒绝属于当前用户但未勾选的购物车条目. */
  @Test
  void testGetCheckoutItemsRejectsUnselectedItem() {
    when(cartItemManager.list(any(Wrapper.class)))
        .thenReturn(List.of(cartItem(1L, 7L, 100L, 2, 0)));

    assertThrows(BizException.class, () -> service.getCheckoutItems(7L, List.of(1L)));
  }

  /** 验证结算清理在购物车数量大于结算数量时保留差额并更新条目. */
  @Test
  void testCleanupCheckoutGreaterQuantityKeepsDifference() {
    CartItemPo item = cartItem(1L, 7L, 100L, 5, 1);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
    when(cartItemManager.updateById(any(CartItemPo.class))).thenReturn(true);

    service.cleanupCheckout(7L, List.of(new CartCheckoutItemDto(1L, 100L, 2)));

    assertEquals(3, item.getQuantity());
    verify(cartItemManager, never()).removeByIds(any());
  }

  /** 验证结算清理遇到 SKU 不匹配时抛异常，且不删除或更新任何条目. */
  @Test
  void testCleanupCheckoutSkuMismatchDeletesNothing() {
    CartItemPo item = cartItem(1L, 7L, 200L, 5, 1);
    when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));

    assertThrows(
        BizException.class,
        () -> service.cleanupCheckout(7L, List.of(new CartCheckoutItemDto(1L, 100L, 2))));

    verify(cartItemManager, never()).removeByIds(any());
    verify(cartItemManager, never()).updateById(any(CartItemPo.class));
  }

  private CartItemPo cartItem(Long id, Long userId, Long skuId, int quantity, int selected) {
    CartItemPo item = new CartItemPo();
    item.setId(id);
    item.setUserId(userId);
    item.setSkuId(skuId);
    item.setQuantity(quantity);
    item.setSelected(selected);
    return item;
  }

  private int runConcurrentUpsert(CountDownLatch start, Long id, int quantity) throws Exception {
    start.await(5, TimeUnit.SECONDS);
    return upsert(id, quantity);
  }

  private int upsert(Long id, int quantity) {
    try (SqlSession session = sqlSessionFactory.openSession(true)) {
      return session.getMapper(CartItemMapper.class).upsertQuantity(id, 7L, 100L, quantity, 1);
    }
  }

  private long queryLong(String sql) {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getLong(1);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private int queryInt(String sql) {
    return Math.toIntExact(queryLong(sql));
  }
}
