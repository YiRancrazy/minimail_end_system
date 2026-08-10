package com.yirancrazy.minimall.notify.service;

import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.service.impl.PlatformCommentServiceImpl;

/**
 * PlatformCommentServiceImpl 单元测试。
 */
public class PlatformCommentServiceImplTest {

    private CommentManager commentManager;
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
        service = new PlatformCommentServiceImpl(commentManager);
    }

    /**
     * 验证 page 委托 manager.list。
     */
    @Test
    public void page_delegates() {
        CommentPageDTO dto = new CommentPageDTO();
        dto.setLimit(20);
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());
        CursorPageVO<CommentPO> r = service.page(dto);
        assertEquals(20, r.getLimit());
        verify(commentManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证 approve 正常状态置为 NORMAL。
     */
    @Test
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
     * 验证 approve 在 NORMAL 状态下幂等返回。
     */
    @Test
    public void approve_idempotent_when_already_normal() {
        CommentPO po = new CommentPO();
        po.setId(1L);
        po.setStatus(CommentStatusEnum.NORMAL.intCode());
        when(commentManager.getById(1L)).thenReturn(po);

        service.approve(1L);

        verify(commentManager, never()).updateById(any(CommentPO.class));
    }

    /**
     * 验证 hide 正常置为 HIDDEN。
     */
    @Test
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
     * 验证 reply 写入回复内容与时间。
     */
    @Test
    public void reply_writes_reply() {
        CommentPO po = new CommentPO();
        po.setId(3L);
        when(commentManager.getById(3L)).thenReturn(po);

        service.reply(3L, "感谢评价");

        assertEquals("感谢评价", po.getMerchantReply());
        assertEquals(po.getMerchantReplyTime() != null, true);
    }

    /**
     * 验证 evaluate 不存在时抛错。
     */
    @Test
    public void approve_throws_when_not_found() {
        when(commentManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.approve(99L));
    }
}
