package com.yirancrazy.minimall.cart.service;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.cart.service.impl.CartServiceImpl;
import com.yirancrazy.minimall.cart.vo.CartItemVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;

/**
 * CartServiceImpl 单元测试，覆盖查询、新增、删除、计数的正常、失败、边界路径。
 */
public class CartServiceImplTest {

    private static DataSource dataSource;
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void initLambdaCache() throws Exception {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            CartItemPO.class);
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("org.h2.Driver");
        driverManagerDataSource.setUrl("jdbc:h2:mem:cart_t13;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        driverManagerDataSource.setUsername("sa");
        dataSource = driverManagerDataSource;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
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
    private UserFeignClient userFeignClient;
    private GoodsFeignClient goodsFeignClient;
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
        userFeignClient = mock(UserFeignClient.class);
        goodsFeignClient = mock(GoodsFeignClient.class);
        minioUtil = mock(MinioUtil.class);
        lenient().doAnswer(inv -> {
            CartItemPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(cartItemManager).save(any(CartItemPO.class));
        service = new CartServiceImpl(cartItemManager, cartItemMapper, userFeignClient, goodsFeignClient, minioUtil);
    }

    /**
     * 验证 listByUser 返回指定用户的购物车条目并叠加商品快照，且快照通过批量接口一次获取。
     */
    @Test
    public void listByUser_returns_items() {
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        item.setSkuId(100L);
        item.setQuantity(2);
        item.setSelected(1);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(goodsFeignClient.batchSkuSnapshot(List.of(100L))).thenReturn(Result.success(
            Map.of(100L, new SkuSnapshotDTO(100L, 10L, "S 白色", new java.math.BigDecimal("9.90"), 99, 7L))));
        when(goodsFeignClient.batchSpuSnapshot(List.of(10L))).thenReturn(Result.success(
            Map.of(10L, new SpuSnapshotDTO(10L, "商品标题", "http://img/a.jpg"))));

        List<CartItemVO> result = service.listByUser(7L);

        assertEquals(1, result.size());
        CartItemVO vo = result.get(0);
        assertEquals(100L, vo.getSkuId());
        assertEquals("商品标题", vo.getTitle());
        assertEquals("S 白色", vo.getSkuSpec());
        verify(goodsFeignClient).batchSkuSnapshot(List.of(100L));
        verify(goodsFeignClient).batchSpuSnapshot(List.of(10L));
        verify(goodsFeignClient, never()).skuSnapshot(any());
        verify(goodsFeignClient, never()).spuSnapshot(any());
    }

    /**
     * 验证用户购物车为空时返回空列表且不调用商品服务。
     */
    @Test
    public void listByUser_returns_empty_when_no_items() {
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        List<CartItemVO> result = service.listByUser(999L);

        assertTrue(result.isEmpty());
        verify(goodsFeignClient, never()).batchSkuSnapshot(any());
        verify(goodsFeignClient, never()).batchSpuSnapshot(any());
    }

    /**
     * 验证商品服务降级返回空快照 Map 时购物车条目仍返回基础字段。
     */
    @Test
    public void listByUser_handles_null_snapshot() {
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        item.setSkuId(100L);
        item.setQuantity(2);
        item.setSelected(1);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(goodsFeignClient.batchSkuSnapshot(any())).thenReturn(Result.success(Collections.emptyMap()));

        List<CartItemVO> result = service.listByUser(7L);

        assertEquals(1, result.size());
        assertNull(result.get(0).getPrice());
        assertNull(result.get(0).getTitle());
        verify(goodsFeignClient, never()).batchSpuSnapshot(any());
    }

    /**
     * 验证多条目购物车通过批量快照接口一次拉取（1 次 SKU + 1 次 SPU），消除逐条目 N+1 远程调用。
     */
    @Test
    public void listByUser_batch_snapshot_called_once_for_multiple_items() {
        CartItemPO item1 = new CartItemPO();
        item1.setId(1L);
        item1.setUserId(7L);
        item1.setSkuId(100L);
        item1.setQuantity(1);
        item1.setSelected(1);
        CartItemPO item2 = new CartItemPO();
        item2.setId(2L);
        item2.setUserId(7L);
        item2.setSkuId(200L);
        item2.setQuantity(2);
        item2.setSelected(1);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item1, item2));
        when(goodsFeignClient.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, new SkuSnapshotDTO(100L, 10L, "S 白色", new java.math.BigDecimal("9.90"), 99, 7L),
            200L, new SkuSnapshotDTO(200L, 10L, "L 黑色", new java.math.BigDecimal("19.90"), 50, 7L))));
        when(goodsFeignClient.batchSpuSnapshot(any())).thenReturn(Result.success(
            Map.of(10L, new SpuSnapshotDTO(10L, "商品标题", "http://img/a.jpg"))));

        List<CartItemVO> result = service.listByUser(7L);

        assertEquals(2, result.size());
        verify(goodsFeignClient).batchSkuSnapshot(any());
        verify(goodsFeignClient).batchSpuSnapshot(any());
        verify(goodsFeignClient, times(0)).skuSnapshot(any());
    }

    /**
     * 验证 add 使用数据库原子 upsert，在 selected 为空时默认选中并返回持久化条目 ID。
     */
    @Test
    public void add_uses_atomic_upsert_and_defaults_selected() {
        CartItemPO persisted = cartItem(9L, 7L, 100L, 2, 1);
        when(cartItemMapper.upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(1))).thenReturn(1);
        when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(persisted);
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(100L);
        dto.setQuantity(2);
        dto.setSelected(null);

        Long id = service.add(7L, dto);

        assertEquals(9L, id);
        verify(cartItemMapper).upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(1));
        verify(cartItemManager, never()).updateById(any(CartItemPO.class));
        verify(cartItemManager, never()).save(any(CartItemPO.class));
    }

    /**
     * 验证 add 透传 selected 原值给数据库 upsert。
     */
    @Test
    public void add_passes_through_selected_when_set() {
        CartItemPO persisted = cartItem(9L, 7L, 100L, 2, 0);
        when(cartItemMapper.upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(0))).thenReturn(1);
        when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(persisted);
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(100L);
        dto.setQuantity(2);
        dto.setSelected(0);

        Long id = service.add(7L, dto);

        assertEquals(9L, id);
        verify(cartItemMapper).upsertQuantity(anyLong(), eq(7L), eq(100L), eq(2), eq(0));
    }

    /**
     * 验证 delete 调用 removeById 并返回其结果。
     */
    @Test
    public void delete_returns_true_on_success() {
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        when(cartItemManager.getById(1L)).thenReturn(item);
        when(cartItemManager.removeById(1L)).thenReturn(true);
        boolean ok = service.delete(1L, 7L);
        assertTrue(ok);
        verify(cartItemManager).removeById(eq(1L));
    }

    /**
     * 验证 delete 在 removeById 失败时返回 false。
     */
    @Test
    public void delete_returns_false_on_failure() {
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        when(cartItemManager.getById(1L)).thenReturn(item);
        when(cartItemManager.removeById(1L)).thenReturn(false);
        boolean ok = service.delete(1L, 7L);
        assertFalse(ok);
    }

    /**
     * 验证 delete 在条目不属于当前用户时抛出 BizException 且不删除。
     */
    @Test
    public void delete_throws_when_user_mismatch() {
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        when(cartItemManager.getById(1L)).thenReturn(item);
        assertThrows(BizException.class, () -> service.delete(1L, 999L));
        verify(cartItemManager, never()).removeById(any());
    }

    /**
     * 验证 countByUser 返回用户购物车条目数量。
     */
    @Test
    public void countByUser_returns_count() {
        when(cartItemManager.count(any())).thenReturn(5L);

        long count = service.countByUser(7L);
        assertEquals(5L, count);
    }

    /**
     * 验证用户无购物车条目时计数返回 0。
     */
    @Test
    public void countByUser_returns_zero_when_empty() {
        when(cartItemManager.count(any())).thenReturn(0L);
        long count = service.countByUser(999L);
        assertEquals(0L, count);
    }

    /**
     * 验证 update 在购物车项不存在时抛出 BizException。
     */
    @Test
    public void update_throws_when_missing() {
        when(cartItemManager.getById(999L)).thenReturn(null);
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(3);
        assertThrows(BizException.class, () -> service.update(999L, 7L, dto));
    }

    /**
     * 验证 update 在条目不属于当前用户时抛出 BizException 且不更新。
     */
    @Test
    public void update_throws_when_user_mismatch() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setUserId(7L);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(3);
        assertThrows(BizException.class, () -> service.update(1L, 999L, dto));
        verify(cartItemManager, never()).updateById(any());
    }

    /**
     * 验证 update 仅更新数量时更新 quantity 并返回 true。
     */
    @Test
    public void update_quantity_only_returns_true_on_success() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setUserId(7L);
        existing.setQuantity(2);
        existing.setSelected(0);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        when(cartItemManager.updateById(any(CartItemPO.class))).thenReturn(true);

        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(5);
        boolean ok = service.update(1L, 7L, dto);

        assertTrue(ok);
        assertEquals(5, existing.getQuantity());
        assertEquals(0, existing.getSelected());
    }

    /**
     * 验证 update 仅更新勾选状态时更新 isSelected 并返回 true。
     */
    @Test
    public void update_selected_only_returns_true_on_success() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setUserId(7L);
        existing.setQuantity(3);
        existing.setSelected(0);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        when(cartItemManager.updateById(any(CartItemPO.class))).thenReturn(true);

        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setIsSelected(true);
        boolean ok = service.update(1L, 7L, dto);

        assertTrue(ok);
        assertEquals(1, existing.getSelected());
        assertEquals(3, existing.getQuantity());
    }

    /**
     * 验证 update 同时更新数量和勾选状态。
     */
    @Test
    public void update_both_fields_returns_true_on_success() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setUserId(7L);
        existing.setQuantity(2);
        existing.setSelected(0);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        when(cartItemManager.updateById(any(CartItemPO.class))).thenReturn(true);

        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(5);
        dto.setIsSelected(true);
        boolean ok = service.update(1L, 7L, dto);

        assertTrue(ok);
        assertEquals(5, existing.getQuantity());
        assertEquals(1, existing.getSelected());
    }

    /**
     * 验证 update 在两个字段都为 null 时不修改任何字段但返回 true。
     */
    @Test
    public void update_no_fields_changed_returns_true_on_success() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setUserId(7L);
        existing.setQuantity(2);
        existing.setSelected(0);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        when(cartItemManager.updateById(any(CartItemPO.class))).thenReturn(true);

        CartUpdateDTO dto = new CartUpdateDTO();
        boolean ok = service.update(1L, 7L, dto);

        assertTrue(ok);
        assertEquals(2, existing.getQuantity());
        assertEquals(0, existing.getSelected());
    }

    /**
     * 验证 add 合并数量由数据库原子 upsert 完成，不执行 read-check-write。
     */
    @Test
    public void add_delegates_merge_to_atomic_upsert() {
        CartItemPO persisted = cartItem(1L, 7L, 100L, 5, 1);
        when(cartItemMapper.upsertQuantity(anyLong(), eq(7L), eq(100L), eq(3), eq(1))).thenReturn(2);
        when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(persisted);
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(100L);
        dto.setQuantity(3);

        Long id = service.add(7L, dto);

        assertEquals(1L, id);
        verify(cartItemMapper).upsertQuantity(anyLong(), eq(7L), eq(100L), eq(3), eq(1));
        verify(cartItemManager, never()).updateById(any(CartItemPO.class));
        verify(cartItemManager, never()).save(any(CartItemPO.class));
    }

    /**
     * 验证 add 不忽略 upsert 失败结果，未更新任何行时抛出系统异常。
     */
    @Test
    public void add_throws_when_upsert_affects_no_rows() {
        when(cartItemMapper.upsertQuantity(anyLong(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(0);
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(100L);
        dto.setQuantity(1);
        dto.setSelected(1);

        BizException exception = assertThrows(BizException.class, () -> service.add(7L, dto));

        assertEquals(CommonCode.SYS_ERROR, exception.getCode());
        verify(cartItemManager, never()).getByUserIdAndSkuId(anyLong(), anyLong());
    }

    /**
     * 验证 upsert 缺少返回行时抛出系统异常，不静默返回错误 ID。
     */
    @Test
    public void add_throws_when_upserted_row_is_not_readable() {
        when(cartItemMapper.upsertQuantity(anyLong(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(1);
        when(cartItemManager.getByUserIdAndSkuId(7L, 100L)).thenReturn(null);
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(100L);
        dto.setQuantity(1);
        dto.setSelected(1);

        BizException exception = assertThrows(BizException.class, () -> service.add(7L, dto));

        assertEquals(CommonCode.SYS_ERROR, exception.getCode());
    }

    /**
     * 验证同一用户同一 SKU 的首次并发 upsert 只产生一条记录并累计两次增量。
     */
    @Test
    public void concurrent_first_insert_produces_single_row_and_accumulates_quantity() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2,
            runnable -> new Thread(runnable, "cart-upsert-test"));
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> runConcurrentUpsert(start, 1001L, 1));
            Future<Integer> second = executor.submit(() -> runConcurrentUpsert(start, 1002L, 1));
            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }
        finally {
            executor.shutdownNow();
        }

        assertEquals(1, queryLong("SELECT COUNT(*) FROM t_cart_item WHERE user_id = 7 AND sku_id = 100"));
        assertEquals(2, queryInt("SELECT quantity FROM t_cart_item WHERE user_id = 7 AND sku_id = 100"));
    }

    /**
     * 验证数据库 upsert 将同 SKU 数量累加后封顶为 999。
     */
    @Test
    public void upsert_caps_quantity_at_999() {
        upsert(1001L, 998);
        upsert(1002L, 5);

        assertEquals(999, queryInt("SELECT quantity FROM t_cart_item WHERE user_id = 7 AND sku_id = 100"));
    }

    /**
     * 验证 selectAll 调用批量更新并返回 true。
     */
    @Test
    public void selectAll_returns_true_on_success() {
        when(cartItemManager.update(any(Wrapper.class))).thenReturn(true);

        CartSelectAllDTO dto = new CartSelectAllDTO();
        dto.setIsSelected(true);
        boolean ok = service.selectAll(7L, dto.getIsSelected() ? 1 : 0);

        assertTrue(ok);
        verify(cartItemManager).update(any(Wrapper.class));
    }

    /**
     * 验证 clear 调用批量删除并返回 true。
     */
    @Test
    public void clear_returns_true_on_success() {
        when(cartItemManager.remove(any(Wrapper.class))).thenReturn(true);

        boolean ok = service.clear(7L);

        assertTrue(ok);
        verify(cartItemManager).remove(any(Wrapper.class));
    }

    /**
     * 验证 moveToFavorite 在收藏成功后按条目 ID 删除对应购物车项。
     */
    @Test
    public void moveToFavorite_success_deletes_cart_item() {
        when(userFeignClient.addFavorite(any(), any())).thenReturn(Result.success(null));
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        item.setSkuId(100L);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(cartItemManager.removeByIds(List.of(1L))).thenReturn(true);

        service.moveToFavorite(7L, List.of(1L));

        verify(userFeignClient).addFavorite(eq(7L), eq(100L));
        verify(cartItemManager).removeByIds(List.of(1L));
    }

    /**
     * 验证 moveToFavorite 在收藏服务失败时抛出 BizException 且不删除购物车项。
     */
    @Test
    public void moveToFavorite_feign_fail_throws() {
        when(userFeignClient.addFavorite(any(), any()))
            .thenReturn(Result.fail(CommonCode.SYS_ERROR, "down"));
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        item.setSkuId(100L);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));

        assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L)));
        verify(cartItemManager, never()).removeByIds(any());
    }

    /**
     * 验证多条目收藏部分失败时购物车完整保留，调用方可按幂等语义安全重试。
     */
    @Test
    public void moveToFavorite_partial_failure_keeps_cart_and_allows_idempotent_retry() {
        CartItemPO first = new CartItemPO();
        first.setId(1L);
        first.setUserId(7L);
        first.setSkuId(100L);
        CartItemPO second = new CartItemPO();
        second.setId(2L);
        second.setUserId(7L);
        second.setSkuId(200L);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(first, second));
        when(userFeignClient.addFavorite(7L, 100L)).thenReturn(Result.success(null));
        when(userFeignClient.addFavorite(7L, 200L))
            .thenReturn(Result.fail(CommonCode.SYS_ERROR, "down"));

        assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L, 2L)));

        verify(cartItemManager, never()).removeByIds(any());
    }

    /**
     * 验证 moveToFavorite 在删除失败时抛出 BizException（事务回滚信号），条目保持原状。
     */
    @Test
    public void moveToFavorite_delete_fail_throws() {
        when(userFeignClient.addFavorite(any(), any())).thenReturn(Result.success(null));
        CartItemPO item = new CartItemPO();
        item.setId(1L);
        item.setUserId(7L);
        item.setSkuId(100L);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(cartItemManager.removeByIds(List.of(1L))).thenReturn(false);

        assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L)));
        verify(userFeignClient).addFavorite(eq(7L), eq(100L));
    }

    /**
     * 验证 moveToFavorite 在条目不存在或不属于当前用户时抛出 BizException 且不收藏、不删除。
     */
    @Test
    public void moveToFavorite_missing_item_throws() {
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of(1L)));
        verify(userFeignClient, never()).addFavorite(any(), any());
        verify(cartItemManager, never()).removeByIds(any());
    }

    /**
     * 验证 moveToFavorite 入参为空列表时抛出 BizException。
     */
    @Test
    public void moveToFavorite_empty_item_ids_throws() {
        assertThrows(BizException.class, () -> service.moveToFavorite(7L, List.of()));
        verify(cartItemManager, never()).list(any(Wrapper.class));
    }

    /**
     * 验证 moveToFavorite 多条目时逐 SKU 收藏且按条目 ID 整体删除。
     */
    @Test
    public void moveToFavorite_multiple_items_favorites_each_sku_and_deletes_by_ids() {
        when(userFeignClient.addFavorite(any(), any())).thenReturn(Result.success(null));
        CartItemPO item1 = new CartItemPO();
        item1.setId(1L);
        item1.setUserId(7L);
        item1.setSkuId(100L);
        CartItemPO item2 = new CartItemPO();
        item2.setId(2L);
        item2.setUserId(7L);
        item2.setSkuId(200L);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item1, item2));
        when(cartItemManager.removeByIds(List.of(1L, 2L))).thenReturn(true);

        service.moveToFavorite(7L, List.of(1L, 2L));

        verify(userFeignClient).addFavorite(eq(7L), eq(100L));
        verify(userFeignClient).addFavorite(eq(7L), eq(200L));
        verify(cartItemManager).removeByIds(List.of(1L, 2L));
    }

    private CartItemPO cartItem(Long id, Long userId, Long skuId, int quantity, int selected) {
        CartItemPO item = new CartItemPO();
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
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private int queryInt(String sql) {
        return Math.toIntExact(queryLong(sql));
    }
}
