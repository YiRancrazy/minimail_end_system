package com.yirancrazy.minimall.notify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.impl.PlatformCommentServiceImpl;
import java.util.Collections;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证平台评价游标查询、游标条件、审核恢复幂等、隐藏、平台回复及评价不存在错误.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class PlatformCommentServiceImplTest {

    private CommentManager commentManager;
    private MinioUtil minioUtil;
    private PlatformCommentServiceImpl service;

    /** 初始化评价表元数据，创建管理器与对象存储 Mock，并构造被测平台评价服务。 */
    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.CommentMapper");
        TableInfoHelper.initTableInfo(assistant, CommentPo.class);

        commentManager = mock(CommentManager.class);
        lenient()
                .when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());
        lenient().when(commentManager.updateById(any(CommentPo.class))).thenReturn(true);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformCommentServiceImpl(commentManager, minioUtil);
    }

    /**
     * 测试目标：验证平台评价查询把请求页大小返回给游标分页对象并调用管理器。
     *
     * 前置：请求 limit 为 20，状态为 NORMAL，管理器返回空集合。
     * 预期：返回分页对象 limit 为 20，且 list 被调用。
     */
    public void pageDelegates() {
        CommentPageDto dto = new CommentPageDto();
        dto.setLimit(20);
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());
        CursorPageVo<CommentPo> r = service.page(dto);
        assertEquals(20, r.getLimit());
        verify(commentManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 测试目标：验证平台评价查询把编码后的主键游标加入查询条件。
     *
     * 前置：游标编码自主键 100，管理器返回空集合。
     * 预期：生成 SQL 条件参数中出现 100。
     */
    public void pageAppliesCursorCondition() {
        CommentPageDto dto = new CommentPageDto();
        dto.setLimit(10);
        dto.setCursor(CursorUtils.encode(100L));
        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());

        service.page(dto);

        ArgumentCaptor<LambdaQueryWrapper<CommentPo>> cap =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(commentManager).list(cap.capture());
        // 条件参数在 SQL 生成（getCustomSqlSegment）时才写入 paramNameValuePairs，需先触发
        cap.getValue().getCustomSqlSegment();
        assertTrue(cap.getValue().getParamNameValuePairs().containsValue(100L));
    }

    /**
     * 测试目标：验证审核接口把隐藏评价恢复为正常状态并持久化。
     *
     * 前置：评价当前状态为 HIDDEN。
     * 预期：评价状态变为 NORMAL，且调用 updateById。
     */
    public void approveNormalizesStatus() {
        CommentPo po = new CommentPo();
        po.setId(1L);
        po.setStatus(CommentStatusEnum.HIDDEN.intCode());
        when(commentManager.getById(1L)).thenReturn(po);

        service.approve(1L);

        assertEquals(CommentStatusEnum.NORMAL.intCode(), po.getStatus());
        verify(commentManager).updateById(po);
    }

    /**
     * 测试目标：验证已经是正常状态的评价再次审核时不重复更新。
     *
     * 前置：评价状态已经是 NORMAL。
     * 预期：不调用 updateById。
     */
    public void approveIdempotentWhenAlreadyNormal() {
        CommentPo po = new CommentPo();
        po.setId(1L);
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        when(commentManager.getById(1L)).thenReturn(po);

        service.approve(1L);

        verify(commentManager, never()).updateById(any(CommentPo.class));
    }

    /**
     * 测试目标：验证隐藏接口把评价状态改为 HIDDEN 并持久化。
     *
     * 前置：评价当前状态为 NORMAL。
     * 预期：评价状态变为 HIDDEN，且调用 updateById。
     */
    public void hideSetsHidden() {
        CommentPo po = new CommentPo();
        po.setId(2L);
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        when(commentManager.getById(2L)).thenReturn(po);

        service.hide(2L);

        assertEquals(CommentStatusEnum.HIDDEN.intCode(), po.getStatus());
        verify(commentManager).updateById(po);
    }

    /**
     * 测试目标：验证平台回复写入评价正文和回复时间。
     *
     * 前置：评价存在。
     * 预期：评价对象包含回复正文和非空回复时间。
     */
    public void replyWritesReply() {
        CommentPo po = new CommentPo();
        po.setId(3L);
        when(commentManager.getById(3L)).thenReturn(po);

        service.reply(3L, "感谢评价");

        assertEquals("感谢评价", po.getMerchantReply());
        assertEquals(po.getMerchantReplyTime() != null, true);
    }

    /**
     * 测试目标：验证审核不存在的评价时返回业务错误。
     *
     * 前置：getById 返回 null。
     * 预期：抛出 BizException。
     */
    public void approveThrowsWhenNotFound() {
        when(commentManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.approve(99L));
    }
}
