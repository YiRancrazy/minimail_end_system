package com.yirancrazy.minimall.user.service;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
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
    private FavoriteServiceImpl service;

    @BeforeEach
    void setUp() {
        userFavoriteManager = mock(UserFavoriteManager.class);
        service = new FavoriteServiceImpl(userFavoriteManager);
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
     * 验证 pageFavorites 将持久化对象转换为 VO 并返回。
     */
    @Test
    public void pageFavorites_returns_vos() {
        FavoritePageDTO dto = new FavoritePageDTO();
        dto.setLimit(10);
        UserFavoritePO po = new UserFavoritePO();
        po.setId(1L);
        po.setUserId(7L);
        po.setSkuId(100L);
        po.setCreateTime(LocalDateTime.now());
        when(userFavoriteManager.list(any(Wrapper.class))).thenReturn(List.of(po));

        CursorPageVO<FavoriteVO> result = service.pageFavorites(7L, dto);

        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getRecords().get(0).getId());
        assertEquals(100L, result.getRecords().get(0).getSkuId());
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
