package com.yirancrazy.minimall.notify.service;

import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.impl.PlatformCommentServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformCommentServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class PlatformCommentServiceImplTest {

    private CommentManager commentManager;
    private MinioUtil minioUtil;
    private PlatformCommentServiceImpl service;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.CommentMapper");
        TableInfoHelper.initTableInfo(assistant, CommentPO.class);

        commentManager = mock(CommentManager.class);
        lenient().when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());
        lenient().when(commentManager.updateById(any(CommentPO.class))).thenReturn(true);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformCommentServiceImpl(commentManager, minioUtil);
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates() {
        CommentPageDTO dto = new CommentPageDTO();
        dto.setLimit(20);
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());
        CursorPageVO<CommentPO> r = service.page(dto);
        assertEquals(20, r.getLimit());
        verify(commentManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 分页查询数据。
     */
    public void page_applies_cursor_condition() {
        CommentPageDTO dto = new CommentPageDTO();
        dto.setLimit(10);
        dto.setCursor(CursorUtils.encode(100L));
        when(commentManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(Collections.emptyList());

        service.page(dto);

        ArgumentCaptor<LambdaQueryWrapper<CommentPO>> cap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(commentManager).list(cap.capture());
        // 条件参数在 SQL 生成（getCustomSqlSegment）时才写入 paramNameValuePairs，需先触发
        cap.getValue().getCustomSqlSegment();
        assertTrue(cap.getValue().getParamNameValuePairs().containsValue(100L));
    }


    /**
     * 执行 approve_normalizes_status 方法。
     */
    public void approve_normalizes_status() {
        CommentPO po = new CommentPO();
        po.setId(1L);
        po.setStatus(CommentStatusEnum.HIDDEN.intCode());
        when(commentManager.getById(1L)).thenReturn(po);

        service.approve(1L);

        assertEquals(CommentStatusEnum.NORMAL.intCode(), po.getStatus());
        verify(commentManager).updateById(po);
    }


    /**
     * 执行 approve_idempotent_when_already_normal 方法。
     */
    public void approve_idempotent_when_already_normal() {
        CommentPO po = new CommentPO();
        po.setId(1L);
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        when(commentManager.getById(1L)).thenReturn(po);

        service.approve(1L);

        verify(commentManager, never()).updateById(any(CommentPO.class));
    }


    /**
     * 执行 hide_sets_hidden 方法。
     */
    public void hide_sets_hidden() {
        CommentPO po = new CommentPO();
        po.setId(2L);
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        when(commentManager.getById(2L)).thenReturn(po);

        service.hide(2L);

        assertEquals(CommentStatusEnum.HIDDEN.intCode(), po.getStatus());
        verify(commentManager).updateById(po);
    }


    /**
     * 执行 reply_writes_reply 方法。
     */
    public void reply_writes_reply() {
        CommentPO po = new CommentPO();
        po.setId(3L);
        when(commentManager.getById(3L)).thenReturn(po);

        service.reply(3L, "感谢评价");

        assertEquals("感谢评价", po.getMerchantReply());
        assertEquals(po.getMerchantReplyTime() != null, true);
    }


    /**
     * 执行 approve_throws_when_not_found 方法。
     */
    public void approve_throws_when_not_found() {
        when(commentManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.approve(99L));
    }
}
