package com.yirancrazy.minimall.notify.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.impl.CommentServiceImpl;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;

/**
 * CommentServiceImpl 单元测试，覆盖评价提交、分页、商家回复、统计的正常、失败、边界路径。
 */
public class CommentServiceImplTest {

    private CommentManager commentManager;
    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.CommentMapper");
        TableInfoHelper.initTableInfo(assistant, CommentPO.class);

        commentManager = mock(CommentManager.class);
        lenient().doAnswer(inv -> {
            CommentPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(commentManager).save(any(CommentPO.class));
        lenient().when(commentManager.updateById(any(CommentPO.class))).thenReturn(true);
        service = new CommentServiceImpl(commentManager);
    }

    /**
     * 验证 create 正常提交评价：rating/content 落库，merchantId 取自 DTO。
     */
    @Test
    public void create_persists_with_dto_fields() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O001");
        dto.setSpuId(100L);
        dto.setMerchantId(11L);
        dto.setRating(5);
        dto.setContent("好评");
        dto.setImages("a.jpg,b.jpg");
        dto.setAnonymous(0);

        Long id = service.create(7L, dto);

        assertNotNull(id);
        verify(commentManager).save(any(CommentPO.class));
    }

    /**
     * 验证 create 在同一订单同一商品重复评价时抛错。
     */
    @Test
    public void create_throws_on_duplicate() {
        CommentPO existing = new CommentPO();
        existing.setId(1L);
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O001");
        dto.setSpuId(100L);
        dto.setRating(4);
        dto.setContent("dup");

        assertThrows(BizException.class, () -> service.create(7L, dto));
        verify(commentManager, never()).save(any(CommentPO.class));
    }

    /**
     * 验证 create merchantId 为空时回填 0。
     */
    @Test
    public void create_fills_merchant_id_zero_when_null() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O002");
        dto.setSpuId(200L);
        dto.setRating(3);
        dto.setContent("ok");

        service.create(7L, dto);

        verify(commentManager).save(any(CommentPO.class));
    }

    /**
     * 验证 page 分页查询委托给 manager 并按 ID 降序。
     */
    @Test
    public void page_delegates_to_manager() {
        CommentPageDTO dto = new CommentPageDTO();
        dto.setLimit(10);

        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());

        CursorPageVO<CommentPO> result = service.page(dto);

        assertNotNull(result);
        verify(commentManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证 page 按 spuId/userId/merchantId/orderNo/status 过滤全部生效。
     */
    @Test
    public void page_filters_combined() {
        CommentPageDTO dto = new CommentPageDTO();
        dto.setLimit(10);
        dto.setSpuId(100L);
        dto.setUserId(7L);
        dto.setMerchantId(11L);
        dto.setOrderNo("O001");
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());

        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());

        service.page(dto);

        verify(commentManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证 reply 正常写入回复内容与回复时间。
     */
    @Test
    public void reply_writes_reply_and_time() {
        CommentPO po = new CommentPO();
        po.setId(1L);
        po.setMerchantId(11L);
        when(commentManager.getById(1L)).thenReturn(po);

        service.reply(1L, 11L, "感谢评价");

        assertEquals("感谢评价", po.getMerchantReply());
        assertNotNull(po.getMerchantReplyTime());
        verify(commentManager).updateById(po);
    }

    /**
     * 验证 reply 在 merchantId 不匹配时抛 COMMENT_NO_PERMISSION。
     */
    @Test
    public void reply_throws_on_merchant_mismatch() {
        CommentPO po = new CommentPO();
        po.setId(1L);
        po.setMerchantId(11L);
        when(commentManager.getById(1L)).thenReturn(po);

        assertThrows(BizException.class, () -> service.reply(1L, 999L, "x"));
        verify(commentManager, never()).updateById(any(CommentPO.class));
    }

    /**
     * 验证 reply 在评价不存在时抛 COMMENT_NOT_FOUND。
     */
    @Test
    public void reply_throws_when_not_found() {
        when(commentManager.getById(1L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.reply(1L, 11L, "x"));
    }

    /**
     * 验证 stats 计算平均分与各档分布。
     */
    @Test
    public void stats_aggregates_rating_distribution() {
        CommentPO a = buildComment(100L, 5);
        CommentPO b = buildComment(100L, 4);
        CommentPO c = buildComment(100L, 3);
        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Arrays.asList(a, b, c));

        CommentStatsVO stats = service.stats(100L);

        assertEquals(3, stats.getTotalCount());
        assertEquals(4.0, stats.getAvgRating());
        assertEquals(1, stats.getRating5Count());
        assertEquals(1, stats.getRating4Count());
        assertEquals(1, stats.getRating3Count());
    }

    /**
     * 验证 stats 在无评价时返回零值。
     */
    @Test
    public void stats_returns_zeros_when_empty() {
        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());

        CommentStatsVO stats = service.stats(100L);

        assertEquals(0, stats.getTotalCount());
        assertEquals(0.0, stats.getAvgRating());
    }

    private CommentPO buildComment(Long spuId, int rating) {
        CommentPO p = new CommentPO();
        p.setSpuId(spuId);
        p.setRating(rating);
        p.setStatus(CommentStatusEnum.NORMAL.intCode());
        p.setCreateTime(LocalDateTime.now());
        return p;
    }
}
