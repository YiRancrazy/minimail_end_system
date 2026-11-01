package com.yirancrazy.minimall.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.dto.FavoritePageDto;
import com.yirancrazy.minimall.user.entity.UserFavoritePo;
import com.yirancrazy.minimall.user.gateway.FavoriteRemoteGateway;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.service.impl.FavoriteServiceImpl;
import com.yirancrazy.minimall.user.vo.FavoriteVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收藏服务单元测试，使用 Manager、商品网关和 MinioUtil 替身覆盖幂等新增、参数校验、分页富化与降级规则.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class FavoriteServiceImplTest {

    /**
     * 初始化 UserFavoritePo 的 MyBatis-Plus Lambda 元数据缓存。
     *
     * <p>该步骤使收藏服务中的 lambda 字段引用可在无 Spring 容器时完成解析。
     */
    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserFavoritePo.class);
    }

    private UserFavoriteManager userFavoriteManager;
    private FavoriteRemoteGateway favoriteRemoteGateway;
    private MinioUtil minioUtil;
    private FavoriteServiceImpl service;

    /**
     * 初始化收藏 Manager、商品网关、MinioUtil 替身和待测服务。
     *
     * <p>MinioUtil 默认原样返回输入，便于断言完整 URL 透传行为。
     */
    @BeforeEach
    void setUp() {
        userFavoriteManager = mock(UserFavoriteManager.class);
        favoriteRemoteGateway = mock(FavoriteRemoteGateway.class);
        minioUtil = mock(MinioUtil.class);
        // 默认按原值返回，等价真实 resolvePublicUrl 对完整 URL 的透传形为
        lenient().when(minioUtil.resolvePublicUrl(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new FavoriteServiceImpl(userFavoriteManager, favoriteRemoteGateway, minioUtil);
    }

    /**
     * 验证用户尚未收藏商品时会写入新的收藏关系。
     *
     * <p>前置条件：用户 7 对 SKU 100 的收藏计数为 0。关键步骤：调用 addFavorite。预期结果：调用
     * save。
     */
    public void addFavoriteSavesWhenNotExists() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(0L);

        service.addFavorite(7L, 100L);

        verify(userFavoriteManager).save(any(UserFavoritePo.class));
    }

    /**
     * 验证重复收藏不会再次写入。
     *
     * <p>前置条件：用户 7 已收藏 SKU 100。关键步骤：再次调用 addFavorite。预期结果：不调用 save。
     */
    public void addFavoriteIdempotentWhenExists() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(1L);

        service.addFavorite(7L, 100L);

        verify(userFavoriteManager, never()).save(any(UserFavoritePo.class));
    }

    /**
     * 验证非法 SKU ID 会阻止新增收藏。
     *
     * <p>前置条件：userId=7，skuId=0。关键步骤：调用 addFavorite。预期结果：抛出 BizException，
     * 且不调用 save。
     */
    public void addFavoriteThrowsWhenSkuIdInvalid() {
        assertThrows(BizException.class, () -> service.addFavorite(7L, 0L));
        verify(userFavoriteManager, never()).save(any(UserFavoritePo.class));
    }

    /**
     * 验证非法用户 ID 会阻止新增收藏。
     *
     * <p>前置条件：userId=0，skuId=100。关键步骤：调用 addFavorite。预期结果：抛出 BizException，
     * 且不调用 save。
     */
    public void addFavoriteThrowsWhenUserIdInvalid() {
        assertThrows(BizException.class, () -> service.addFavorite(0L, 100L));
        verify(userFavoriteManager, never()).save(any(UserFavoritePo.class));
    }

    /**
     * 验证合法参数会触发按用户和 SKU 的条件删除。
     *
     * <p>前置条件：userId=7，skuId=100。关键步骤：调用 removeFavorite。预期结果：调用 remove 且传入
     * Wrapper。
     */
    public void removeFavoriteCallsRemove() {
        service.removeFavorite(7L, 100L);
        verify(userFavoriteManager).remove(any(Wrapper.class));
    }

    /**
     * 验证非法 SKU ID 会阻止取消收藏。
     *
     * <p>前置条件：userId=7，skuId=-1。关键步骤：调用 removeFavorite。预期结果：抛出 BizException，
     * 且不调用 remove。
     */
    public void removeFavoriteThrowsWhenSkuIdInvalid() {
        assertThrows(BizException.class, () -> service.removeFavorite(7L, -1L));
        verify(userFavoriteManager, never()).remove(any(Wrapper.class));
    }

    /**
     * 验证收藏分页会用当前页 SKU 和 SPU 快照富化商品字段。
     *
     * <p>前置条件：Manager 返回一条收藏，商品网关返回对应 SKU 和 SPU 快照。关键步骤：调用
     * pageFavorites。预期结果：视图包含 SPU ID、名称、价格和解析后的图片 URL。
     */
    public void pageFavoritesEnrichesWithGoodsSnapshot() {
        FavoritePageDto dto = new FavoritePageDto();
        dto.setLimit(10);
        UserFavoritePo po = new UserFavoritePo();
        po.setId(1L);
        po.setUserId(7L);
        po.setSkuId(100L);
        po.setCreateTime(LocalDateTime.now());
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of(po));

        SkuSnapshotDto sku = new SkuSnapshotDto(100L, 200L, "无线鼠标", BigDecimal.valueOf(199.00), 5, 3L);
        SpuSnapshotDto spu = new SpuSnapshotDto(200L, "无线鼠标旗舰版", "http://img/200.jpg");
        when(favoriteRemoteGateway.batchSkuSnapshot(List.of(100L)))
                .thenReturn(Result.success(Map.of(100L, sku)));
        when(favoriteRemoteGateway.batchSpuSnapshot(List.of(200L)))
                .thenReturn(Result.success(Map.of(200L, spu)));

        CursorPageVo<FavoriteVo> result = service.pageFavorites(7L, dto);

        FavoriteVo vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSkuId());
        assertEquals(200L, vo.getSpuId());
        assertEquals("无线鼠标", vo.getSkuName());
        assertEquals("http://img/200.jpg", vo.getSkuImage());
        assertEquals(BigDecimal.valueOf(199.00), vo.getPrice());
    }

    /**
     * 验证商品快照降级为空 Map 时仍返回收藏基础记录。
     *
     * <p>前置条件：Manager 返回一条收藏，SKU 批量查询返回空 Map。关键步骤：调用 pageFavorites。
     * 预期结果：收藏 ID 和 SKU ID 保留，商品名称、图片和价格为空。
     */
    public void pageFavoritesDegradesWhenGoodsSnapshotMissing() {
        FavoritePageDto dto = new FavoritePageDto();
        dto.setLimit(10);
        UserFavoritePo po = new UserFavoritePo();
        po.setId(1L);
        po.setUserId(7L);
        po.setSkuId(100L);
        po.setCreateTime(LocalDateTime.now());
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of(po));
        // 商品服务不可用时 fallback 返回空 Map
        when(favoriteRemoteGateway.batchSkuSnapshot(List.of(100L)))
                .thenReturn(Result.success(Map.of()));

        CursorPageVo<FavoriteVo> result = service.pageFavorites(7L, dto);

        FavoriteVo vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSkuId());
        assertNull(vo.getSkuName());
        assertNull(vo.getSkuImage());
        assertNull(vo.getPrice());
    }

    /**
     * 验证没有收藏记录时不会发起商品快照查询。
     *
     * <p>前置条件：Manager 返回空列表。关键步骤：调用 pageFavorites。预期结果：分页 records 为空，
     * 且不调用 batchSkuSnapshot。
     */
    public void pageFavoritesReturnsEmptyWhenNoRecords() {
        FavoritePageDto dto = new FavoritePageDto();
        dto.setLimit(10);
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of());

        CursorPageVo<FavoriteVo> result = service.pageFavorites(7L, dto);

        assertEquals(0, result.getRecords().size());
        verify(favoriteRemoteGateway, never()).batchSkuSnapshot(any());
    }

    /**
     * 验证收藏数量直接反映 Manager 的计数结果。
     *
     * <p>前置条件：用户 7 的计数为 5。关键步骤：调用 countFavorites。预期结果：返回 5。
     */
    public void countFavoritesReturnsCount() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(5L);

        int count = service.countFavorites(7L);

        assertEquals(5, count);
    }

    /**
     * 验证没有收藏时数量为零。
     *
     * <p>前置条件：用户 999 的计数为 0。关键步骤：调用 countFavorites。预期结果：返回 0。
     */
    public void countFavoritesReturnsZeroWhenEmpty() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(0L);

        int count = service.countFavorites(999L);

        assertEquals(0, count);
    }
}
