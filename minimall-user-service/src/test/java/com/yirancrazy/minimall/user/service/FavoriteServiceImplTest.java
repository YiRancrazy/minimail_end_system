package com.yirancrazy.minimall.user.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.dto.FavoritePageDTO;
import com.yirancrazy.minimall.user.entity.UserFavoritePO;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.service.impl.FavoriteServiceImpl;
import com.yirancrazy.minimall.user.vo.FavoriteVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: FavoriteServiceImpl 单元测试，覆盖收藏增删查的正常、失败、边界路径
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public class FavoriteServiceImplTest {

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            UserFavoritePO.class);
    }

    private UserFavoriteManager userFavoriteManager;
    private GoodsFeignClient goodsFeignClient;
    private MinioUtil minioUtil;
    private FavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        userFavoriteManager = mock(UserFavoriteManager.class);
        goodsFeignClient = mock(GoodsFeignClient.class);
        minioUtil = mock(MinioUtil.class);
        // 默认按原值返回，等价真实 resolvePublicUrl 对完整 URL 的透传形为
        lenient().when(minioUtil.resolvePublicUrl(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new FavoriteServiceImpl(userFavoriteManager, goodsFeignClient, minioUtil);
    }

    /**
     * 验证 addFavorite 在未收藏时持久化新收藏。
     */
    @Test
    public void addFavorite_saves_when_not_exists() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(0L);

        service.addFavorite(7L, 100L);

        verify(userFavoriteManager).save(any(UserFavoritePO.class));
    }

    /**
     * 验证 addFavorite 在已收藏时幂等返回且不重复插入。
     */
    @Test
    public void addFavorite_idempotent_when_exists() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(1L);

        service.addFavorite(7L, 100L);

        verify(userFavoriteManager, never()).save(any(UserFavoritePO.class));
    }

    /**
     * 验证 addFavorite 在 skuId 非法时抛出 BizException。
     */
    @Test
    public void addFavorite_throws_when_skuId_invalid() {
        assertThrows(BizException.class, () -> service.addFavorite(7L, 0L));
        verify(userFavoriteManager, never()).save(any(UserFavoritePO.class));
    }

    /**
     * 验证 addFavorite 在 userId 非法时抛出 BizException。
     */
    @Test
    public void addFavorite_throws_when_userId_invalid() {
        assertThrows(BizException.class, () -> service.addFavorite(0L, 100L));
        verify(userFavoriteManager, never()).save(any(UserFavoritePO.class));
    }

    /**
     * 验证 removeFavorite 调用按条件删除。
     */
    @Test
    public void removeFavorite_calls_remove() {
        service.removeFavorite(7L, 100L);
        verify(userFavoriteManager).remove(any(Wrapper.class));
    }

    /**
     * 验证 removeFavorite 在 skuId 非法时抛出 BizException。
     */
    @Test
    public void removeFavorite_throws_when_skuId_invalid() {
        assertThrows(BizException.class, () -> service.removeFavorite(7L, -1L));
        verify(userFavoriteManager, never()).remove(any(Wrapper.class));
    }

    /**
     * 验证 pageFavorites 用 SKU/SPU 快照富化商品标题、主图与价格。
     */
    @Test
    public void pageFavorites_enriches_with_goods_snapshot() {
        FavoritePageDTO dto = new FavoritePageDTO();
        dto.setLimit(10);
        UserFavoritePO po = new UserFavoritePO();
        po.setId(1L);
        po.setUserId(7L);
        po.setSkuId(100L);
        po.setCreateTime(LocalDateTime.now());
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of(po));

        SkuSnapshotDTO sku = new SkuSnapshotDTO(100L, 200L, "无线鼠标", BigDecimal.valueOf(199.00), 5, 3L);
        SpuSnapshotDTO spu = new SpuSnapshotDTO(200L, "无线鼠标旗舰版", "http://img/200.jpg");
        when(goodsFeignClient.batchSkuSnapshot(List.of(100L))).thenReturn(Result.success(Map.of(100L, sku)));
        when(goodsFeignClient.batchSpuSnapshot(List.of(200L))).thenReturn(Result.success(Map.of(200L, spu)));

        CursorPageVO<FavoriteVO> result = service.pageFavorites(7L, dto);

        FavoriteVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSkuId());
        assertEquals(200L, vo.getSpuId());
        assertEquals("无线鼠标", vo.getSkuName());
        assertEquals("http://img/200.jpg", vo.getSkuImage());
        assertEquals(BigDecimal.valueOf(199.00), vo.getPrice());
    }

    /**
     * 验证商品快照缺失时收藏核心字段仍返回，商品展示字段保持 null。
     */
    @Test
    public void pageFavorites_degrades_when_goods_snapshot_missing() {
        FavoritePageDTO dto = new FavoritePageDTO();
        dto.setLimit(10);
        UserFavoritePO po = new UserFavoritePO();
        po.setId(1L);
        po.setUserId(7L);
        po.setSkuId(100L);
        po.setCreateTime(LocalDateTime.now());
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of(po));
        // 商品服务不可用时 fallback 返回空 Map
        when(goodsFeignClient.batchSkuSnapshot(List.of(100L))).thenReturn(Result.success(Map.of()));

        CursorPageVO<FavoriteVO> result = service.pageFavorites(7L, dto);

        FavoriteVO vo = result.getRecords().get(0);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSkuId());
        assertNull(vo.getSkuName());
        assertNull(vo.getSkuImage());
        assertNull(vo.getPrice());
    }

    /**
     * 验证无收藏且无商品快照请求时返回空分页。
     */
    @Test
    public void pageFavorites_returns_empty_when_no_records() {
        FavoritePageDTO dto = new FavoritePageDTO();
        dto.setLimit(10);
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of());

        CursorPageVO<FavoriteVO> result = service.pageFavorites(7L, dto);

        assertEquals(0, result.getRecords().size());
        verify(goodsFeignClient, never()).batchSkuSnapshot(any());
    }

    /**
     * 验证 countFavorites 返回用户收藏数量。
     */
    @Test
    public void countFavorites_returns_count() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(5L);

        int count = service.countFavorites(7L);

        assertEquals(5, count);
    }

    /**
     * 验证用户无收藏时 countFavorites 返回 0。
     */
    @Test
    public void countFavorites_returns_zero_when_empty() {
        when(userFavoriteManager.count(any(Wrapper.class))).thenReturn(0L);

        int count = service.countFavorites(999L);

        assertEquals(0, count);
    }
}
