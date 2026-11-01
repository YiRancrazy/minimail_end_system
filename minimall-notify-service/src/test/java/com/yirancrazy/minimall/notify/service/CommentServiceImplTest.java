package com.yirancrazy.minimall.notify.service;

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
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.dto.CommentCreateDto;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.gateway.CommentRemoteGateway;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.impl.CommentServiceImpl;
import com.yirancrazy.minimall.notify.vo.CommentStatsVo;
import java.util.Arrays;
import java.util.Collections;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mockito 验证评价创建的资格与重复校验、游标查询过滤、商家回复归属以及评分统计聚合逻辑.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class CommentServiceImplTest {

    private CommentManager commentManager;
    private CommentRemoteGateway commentRemoteGateway;
    private MinioUtil minioUtil;
    private CommentServiceImpl service;

    /** 初始化评价表元数据，创建管理器和远程网关 Mock，并构造被测评价服务。 */
    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.CommentMapper");
        TableInfoHelper.initTableInfo(assistant, CommentPo.class);

        commentManager = mock(CommentManager.class);
        lenient()
                .doAnswer(
                        inv -> {
                            CommentPo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(commentManager)
                .save(any(CommentPo.class));
        lenient().when(commentManager.updateById(any(CommentPo.class))).thenReturn(true);
        commentRemoteGateway = mock(CommentRemoteGateway.class);
        lenient()
                .when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
                .thenReturn(Result.success(new OrderCommentEligibilityDto(true, 1L, 7L, 11L, 4)));
        minioUtil = mock(MinioUtil.class);
        service = new CommentServiceImpl(commentManager, commentRemoteGateway, minioUtil);
    }

    /**
     * 测试目标：验证评价创建会保存请求字段并使用订单资格结果覆盖商家编号。
     *
     * 前置：数据库中无重复评价，订单资格返回可评价且商家编号为 11。
     * 预期：返回非空主键，保存的评价商家编号为 11 而不是请求体中的 99。
     */
    public void createPersistsWithDtoFields() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        CommentCreateDto dto = new CommentCreateDto();
        dto.setOrderNo("O001");
        dto.setSpuId(100L);
        dto.setMerchantId(99L);
        dto.setRating(5);
        dto.setContent("好评");
        dto.setImages("a.jpg,b.jpg");
        dto.setAnonymous(0);

        Long id = service.create(7L, dto);

        assertNotNull(id);
        ArgumentCaptor<CommentPo> captor = ArgumentCaptor.forClass(CommentPo.class);
        verify(commentManager).save(captor.capture());
        assertEquals(11L, captor.getValue().getMerchantId());
    }

    /**
     * 测试目标：验证写入前发现同一用户、订单和 SPU 的已有评价时拒绝重复创建。
     *
     * 前置：CommentManager.getOne 返回已有评价。
     * 预期：抛出 BizException，且不再执行 save。
     */
    public void createThrowsOnDuplicate() {
        CommentPo existing = new CommentPo();
        existing.setId(1L);
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        CommentCreateDto dto = new CommentCreateDto();
        dto.setOrderNo("O001");
        dto.setSpuId(100L);
        dto.setRating(4);
        dto.setContent("dup");

        assertThrows(BizException.class, () -> service.create(7L, dto));
        verify(commentManager, never()).save(any(CommentPo.class));
    }

    /**
     * 测试目标：验证订单资格结果缺少商家编号时保存 0 作为兜底商家归属。
     *
     * 前置：订单资格可评价，但返回的 merchantId 为 null。
     * 预期：保存的评价 merchantId 为 0。
     */
    public void createFillsMerchantIdZeroWhenNull() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        CommentCreateDto dto = new CommentCreateDto();
        dto.setOrderNo("O002");
        dto.setSpuId(200L);
        dto.setMerchantId(88L);
        dto.setRating(3);
        dto.setContent("ok");
        when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
                .thenReturn(Result.success(new OrderCommentEligibilityDto(true, 1L, 7L, null, 4)));

        service.create(7L, dto);

        ArgumentCaptor<CommentPo> captor = ArgumentCaptor.forClass(CommentPo.class);
        verify(commentManager).save(captor.capture());
        assertEquals(0L, captor.getValue().getMerchantId());
    }

    /**
     * 测试目标：验证订单商品不具备购买资格时拒绝创建评价。
     *
     * 前置：订单资格 eligible 为 false。
     * 预期：抛出 COMMENT_ORDER_NOT_PURCHASED，且不保存评价。
     */
    public void createRejectsWhenOrderNotEligible() {
        when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
                .thenReturn(Result.success(new OrderCommentEligibilityDto(false, 1L, 7L, 11L, 1)));

        CommentCreateDto dto = new CommentCreateDto();
        dto.setOrderNo("O003");
        dto.setSpuId(300L);
        dto.setRating(5);
        dto.setContent("伪造评价");

        BizException ex = assertThrows(BizException.class, () -> service.create(7L, dto));

        assertEquals(NotifyCodeEnum.COMMENT_ORDER_NOT_PURCHASED.getCode(), ex.getCode());
        verify(commentManager, never()).save(any(CommentPo.class));
    }

    /**
     * 测试目标：验证订单资格远程服务返回失败时转换为评价服务不可用错误。
     *
     * 前置：CommentRemoteGateway 返回失败 Result。
     * 预期：抛出 COMMENT_ORDER_SERVICE_UNAVAILABLE，且不保存评价。
     */
    public void createRejectsWhenOrderServiceUnavailable() {
        when(commentRemoteGateway.commentEligibility(anyString(), any(), any()))
                .thenReturn(Result.fail("20000", "订单服务不可用"));

        CommentCreateDto dto = new CommentCreateDto();
        dto.setOrderNo("O004");
        dto.setSpuId(400L);
        dto.setRating(5);
        dto.setContent("降级评价");

        BizException ex = assertThrows(BizException.class, () -> service.create(7L, dto));

        assertEquals(NotifyCodeEnum.COMMENT_ORDER_SERVICE_UNAVAILABLE.getCode(), ex.getCode());
        verify(commentManager, never()).save(any(CommentPo.class));
    }

    /**
     * 测试目标：验证并发重复提交由数据库唯一索引兜底转换为业务异常。
     *
     * 前置：写入前查询未发现重复，但 CommentManager.save 抛出 DuplicateKeyException。
     * 预期：抛出 COMMENT_ALREADY_EXISTS。
     */
    public void createRejectsDuplicateRejectedByUniqueIndex() {
        when(commentManager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);
        when(commentManager.save(any(CommentPo.class)))
                .thenThrow(new DuplicateKeyException("uk_notify_comment_order_spu_user"));

        CommentCreateDto dto = new CommentCreateDto();
        dto.setOrderNo("O005");
        dto.setSpuId(500L);
        dto.setRating(5);
        dto.setContent("并发评价");

        BizException ex = assertThrows(BizException.class, () -> service.create(7L, dto));

        assertEquals(NotifyCodeEnum.COMMENT_ALREADY_EXISTS.getCode(), ex.getCode());
    }

    /**
     * 测试目标：验证评价游标查询会调用管理器并返回非空分页对象。
     *
     * 前置：CommentManager.list 返回空集合，请求 limit 为 10。
     * 预期：返回非空 CursorPageVo，且管理器 list 被调用一次。
     */
    public void pageDelegatesToManager() {
        CommentPageDto dto = new CommentPageDto();
        dto.setLimit(10);

        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());

        CursorPageVo<CommentPo> result = service.page(dto);

        assertNotNull(result);
        verify(commentManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 测试目标：验证多个筛选条件组合时仍通过管理器执行一次评价查询。
     *
     * 前置：请求同时设置 SPU、用户、商家、订单号和状态过滤。
     * 预期：CommentManager.list 被调用。
     */
    public void pageFiltersCombined() {
        CommentPageDto dto = new CommentPageDto();
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
     * 测试目标：验证商家回复会写入正文和回复时间并持久化。
     *
     * 前置：评价存在且 merchantId 与当前商家一致。
     * 预期：评价对象包含回复正文和非空回复时间，并调用 updateById。
     */
    public void replyWritesReplyAndTime() {
        CommentPo po = new CommentPo();
        po.setId(1L);
        po.setMerchantId(11L);
        when(commentManager.getById(1L)).thenReturn(po);

        service.reply(1L, 11L, "感谢评价");

        assertEquals("感谢评价", po.getMerchantReply());
        assertNotNull(po.getMerchantReplyTime());
        verify(commentManager).updateById(po);
    }

    /**
     * 测试目标：验证商家不能回复其他商家的评价。
     *
     * 前置：评价归属商家 11，请求商家为 999。
     * 预期：抛出 BizException，且不调用 updateById。
     */
    public void replyThrowsOnMerchantMismatch() {
        CommentPo po = new CommentPo();
        po.setId(1L);
        po.setMerchantId(11L);
        when(commentManager.getById(1L)).thenReturn(po);

        assertThrows(BizException.class, () -> service.reply(1L, 999L, "x"));
        verify(commentManager, never()).updateById(any(CommentPo.class));
    }

    /**
     * 测试目标：验证对不存在的评价回复时返回业务错误。
     *
     * 前置：CommentManager.getById 返回 null。
     * 预期：抛出 BizException。
     */
    public void replyThrowsWhenNotFound() {
        when(commentManager.getById(1L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.reply(1L, 11L, "x"));
    }

    /**
     * 测试目标：验证评分统计正确累计总数、平均分和各评分数量。
     *
     * 前置：正常评价统计返回 5 分、4 分、3 分各 1 条。
     * 预期：总数 3、平均分 4.0、三个评分档均为 1，且不执行列表查询。
     */
    public void statsAggregatesRatingDistribution() {
        CommentRatingCountDto r5 = ratingCount(5, 1L);
        CommentRatingCountDto r4 = ratingCount(4, 1L);
        CommentRatingCountDto r3 = ratingCount(3, 1L);
        when(commentManager.countGroupByRating(100L, CommentStatusEnum.NORMAL.intCode()))
                .thenReturn(Arrays.asList(r5, r4, r3));

        CommentStatsVo stats = service.stats(100L);

        assertEquals(3, stats.getTotalCount());
        assertEquals(4.0, stats.getAvgRating());
        assertEquals(1, stats.getRating5Count());
        assertEquals(1, stats.getRating4Count());
        assertEquals(1, stats.getRating3Count());
        verify(commentManager).countGroupByRating(100L, CommentStatusEnum.NORMAL.intCode());
        verify(commentManager, never()).list(any(Wrapper.class));
    }

    /** 构造指定评分和数量的评分分组统计记录，供聚合测试使用. */
    private CommentRatingCountDto ratingCount(int rating, long cnt) {
        CommentRatingCountDto dto = new CommentRatingCountDto();
        dto.setRating(rating);
        dto.setCnt(cnt);
        return dto;
    }

    /**
     * 测试目标：验证 SPU 无正常评价时统计结果返回全零。
     *
     * 前置：评分分组统计返回空集合。
     * 预期：总数为 0，平均分为 0.0。
     */
    public void statsReturnsZerosWhenEmpty() {
        when(commentManager.countGroupByRating(100L, CommentStatusEnum.NORMAL.intCode()))
                .thenReturn(Collections.emptyList());

        CommentStatsVo stats = service.stats(100L);

        assertEquals(0, stats.getTotalCount());
        assertEquals(0.0, stats.getAvgRating());
    }
}
