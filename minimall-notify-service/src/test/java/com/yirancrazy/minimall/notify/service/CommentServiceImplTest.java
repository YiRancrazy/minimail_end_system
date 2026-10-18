package com.yirancrazy.minimall.notify.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.gateway.CommentRemoteGateway;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.impl.CommentServiceImpl;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class CommentServiceImplTest {

    private CommentManager commentManager;
    private CommentRemoteGateway commentRemoteGateway;
    private MinioUtil minioUtil;
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
        commentRemoteGateway = mock(CommentRemoteGateway.class);
        lenient().when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
            .thenReturn(Result.success(new OrderCommentEligibilityDTO(true, 1L, 7L, 11L, 4)));
        minioUtil = mock(MinioUtil.class);
        service = new CommentServiceImpl(commentManager, commentRemoteGateway, minioUtil);
    }


    /**
     * 创建业务数据。
     */
    public void create_persists_with_dto_fields() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O001");
        dto.setSpuId(100L);
        dto.setMerchantId(99L);
        dto.setRating(5);
        dto.setContent("好评");
        dto.setImages("a.jpg,b.jpg");
        dto.setAnonymous(0);

        Long id = service.create(7L, dto);

        assertNotNull(id);
        ArgumentCaptor<CommentPO> captor = ArgumentCaptor.forClass(CommentPO.class);
        verify(commentManager).save(captor.capture());
        assertEquals(11L, captor.getValue().getMerchantId());
    }


    /**
     * 创建业务数据。
     */
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
     * 创建业务数据。
     */
    public void create_fills_merchant_id_zero_when_null() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O002");
        dto.setSpuId(200L);
        dto.setMerchantId(88L);
        dto.setRating(3);
        dto.setContent("ok");
        when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
            .thenReturn(Result.success(new OrderCommentEligibilityDTO(true, 1L, 7L, null, 4)));

        service.create(7L, dto);

        ArgumentCaptor<CommentPO> captor = ArgumentCaptor.forClass(CommentPO.class);
        verify(commentManager).save(captor.capture());
        assertEquals(0L, captor.getValue().getMerchantId());
    }


    /**
     * 创建业务数据。
     */
    public void create_rejects_when_order_not_eligible() {
        when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
            .thenReturn(Result.success(new OrderCommentEligibilityDTO(false, 1L, 7L, 11L, 1)));

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O003");
        dto.setSpuId(300L);
        dto.setRating(5);
        dto.setContent("伪造评价");

        BizException ex = assertThrows(BizException.class, () -> service.create(7L, dto));

        assertEquals(NotifyCodeEnum.COMMENT_ORDER_NOT_PURCHASED.getCode(), ex.getCode());
        verify(commentManager, never()).save(any(CommentPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void create_rejects_when_order_service_unavailable() {
        when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
            .thenReturn(Result.fail("20000", "订单服务不可用"));

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O004");
        dto.setSpuId(400L);
        dto.setRating(5);
        dto.setContent("降级评价");

        BizException ex = assertThrows(BizException.class, () -> service.create(7L, dto));

        assertEquals(NotifyCodeEnum.COMMENT_ORDER_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        verify(commentManager, never()).save(any(CommentPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void create_rejects_duplicate_rejected_by_unique_index() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);
        when(commentManager.save(any(CommentPO.class)))
            .thenThrow(new DuplicateKeyException("uk_notify_comment_order_spu_user"));

        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setOrderNo("O005");
        dto.setSpuId(500L);
        dto.setRating(5);
        dto.setContent("并发评价");

        BizException ex = assertThrows(BizException.class, () -> service.create(7L, dto));

        assertEquals(NotifyCodeEnum.COMMENT_ALREADY_EXISTS.getCode(), ex.getCode());
    }


    /**
     * 分页查询数据。
     */
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
     * 分页查询数据。
     */
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
     * 执行 reply_writes_reply_and_time 方法。
     */
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
     * 执行 reply_throws_on_merchant_mismatch 方法。
     */
    public void reply_throws_on_merchant_mismatch() {
        CommentPO po = new CommentPO();
        po.setId(1L);
        po.setMerchantId(11L);
        when(commentManager.getById(1L)).thenReturn(po);

        assertThrows(BizException.class, () -> service.reply(1L, 999L, "x"));
        verify(commentManager, never()).updateById(any(CommentPO.class));
    }


    /**
     * 执行 reply_throws_when_not_found 方法。
     */
    public void reply_throws_when_not_found() {
        when(commentManager.getById(1L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.reply(1L, 11L, "x"));
    }


    /**
     * 执行 stats_aggregates_rating_distribution 方法。
     */
    public void stats_aggregates_rating_distribution() {
        CommentRatingCountDTO r5 = ratingCount(5, 1L);
        CommentRatingCountDTO r4 = ratingCount(4, 1L);
        CommentRatingCountDTO r3 = ratingCount(3, 1L);
        when(commentManager.countGroupByRating(100L, CommentStatusEnum.NORMAL.intCode()))
                .thenReturn(Arrays.asList(r5, r4, r3));

        CommentStatsVO stats = service.stats(100L);

        assertEquals(3, stats.getTotalCount());
        assertEquals(4.0, stats.getAvgRating());
        assertEquals(1, stats.getRating5Count());
        assertEquals(1, stats.getRating4Count());
        assertEquals(1, stats.getRating3Count());
        verify(commentManager).countGroupByRating(100L, CommentStatusEnum.NORMAL.intCode());
        verify(commentManager, never()).list(any(Wrapper.class));
    }

    private CommentRatingCountDTO ratingCount(int rating, long cnt) {
        CommentRatingCountDTO dto = new CommentRatingCountDTO();
        dto.setRating(rating);
        dto.setCnt(cnt);
        return dto;
    }


    /**
     * 执行 stats_returns_zeros_when_empty 方法。
     */
    public void stats_returns_zeros_when_empty() {
        when(commentManager.countGroupByRating(100L, CommentStatusEnum.NORMAL.intCode()))
                .thenReturn(Collections.emptyList());

        CommentStatsVO stats = service.stats(100L);

        assertEquals(0, stats.getTotalCount());
        assertEquals(0.0, stats.getAvgRating());
    }
}
