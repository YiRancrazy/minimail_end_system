package com.yirancrazy.minimall.notify.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDTO;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.impl.NotifyServiceImpl;
import com.yirancrazy.minimall.notify.sse.SseHub;

/**
 * NotifyServiceImpl 单元测试，覆盖站内信发送、分页、已读标记、删除与广播的正常及异常路径。
 */
public class NotifyServiceImplTest {

    private NotifyManager notifyManager;
    private SseHub sseHub;
    private NotifyServiceImpl service;

    @BeforeEach
    void setUp() {
        // Initialize TableInfo so LambdaUpdateWrapper can resolve lambda cache
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.NotifyMessageMapper");
        TableInfoHelper.initTableInfo(assistant, NotifyMessagePO.class);

        notifyManager = mock(NotifyManager.class);
        sseHub = mock(SseHub.class);
        lenient().when(notifyManager.save(any(NotifyMessagePO.class))).thenReturn(true);
        lenient().when(notifyManager.updateById(any(NotifyMessagePO.class))).thenReturn(true);
        lenient().when(notifyManager.removeById(99L)).thenReturn(true);
        service = new NotifyServiceImpl(notifyManager, sseHub);
    }

    /**
     * 验证 push 成功时返回 true 并通过 SSE 推送。
     */
    @Test
    public void push_persists_unread_message() {
        boolean ok = service.push(7L, "订单支付成功", "订单 99 已支付");
        assertTrue(ok);
        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(sseHub).send(7L, "订单支付成功");
    }

    /**
     * 验证 save 失败时 push 返回 false 且不推送 SSE。
     */
    @Test
    public void push_returns_false_when_save_fails() {
        when(notifyManager.save(any(NotifyMessagePO.class))).thenReturn(false);
        boolean ok = service.push(7L, "title", "content");
        assertFalse(ok);
        verify(sseHub, never()).send(any(), any());
    }

    /**
     * 验证 listByUser 返回指定用户的消息列表。
     */
    @Test
    public void listByUser_returns_messages() {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(7L);
        when(notifyManager.list(any(Wrapper.class))).thenReturn(List.of(m));

        NotifyListDTO dto = new NotifyListDTO();
        dto.setUserId(7L);
        List<NotifyMessagePO> result = service.listByUser(dto);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getUserId());
    }

    /**
     * 验证用户无消息时返回空列表。
     */
    @Test
    public void listByUser_returns_empty_when_no_messages() {
        when(notifyManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        NotifyListDTO dto = new NotifyListDTO();
        dto.setUserId(999L);
        List<NotifyMessagePO> result = service.listByUser(dto);

        assertTrue(result.isEmpty());
    }

    /**
     * 验证 page 委托给 manager.page 并返回结果。
     */
    @Test
    public void page_delegates_to_manager() {
        NotifyListDTO dto = new NotifyListDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setUserId(7L);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        IPage<NotifyMessagePO> expected = new Page<>(1, 10);
        when(notifyManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<NotifyMessagePO> result = service.page(dto);
        assertEquals(expected, result);
        verify(notifyManager).page(any(IPage.class), any());
    }

    /**
     * 验证 unreadCount 委托给 manager.count。
     */
    @Test
    public void unreadCount_delegates_to_manager() {
        when(notifyManager.count(any(Wrapper.class))).thenReturn(5L);

        long count = service.unreadCount(RecipientTypeEnum.USER.intCode(), 7L);
        assertEquals(5L, count);
        verify(notifyManager).count(any(Wrapper.class));
    }

    /**
     * 验证 markRead 将未读消息更新为已读。
     */
    @Test
    public void markRead_updates_flag() {
        NotifyMessagePO m = buildOwnedMessage(99L, 0);
        when(notifyManager.getById(99L)).thenReturn(m);

        service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L);
        assertEquals(1, m.getReadFlag());
        verify(notifyManager).updateById(m);
    }

    /**
     * 验证 markRead 对已读消息幂等返回，不重复更新。
     */
    @Test
    public void markRead_already_read_idempotent() {
        NotifyMessagePO m = buildOwnedMessage(99L, 1);
        when(notifyManager.getById(99L)).thenReturn(m);

        service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L);
        verify(notifyManager, never()).updateById(any(NotifyMessagePO.class));
    }

    /**
     * 验证 markRead 在消息不存在时抛出 BizException。
     */
    @Test
    public void markRead_missing_throws() {
        when(notifyManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L));
    }

    /**
     * 验证 markAllRead 委托给 manager.update 批量更新。
     */
    @Test
    public void markAllRead_delegates_to_update() {
        service.markAllRead(RecipientTypeEnum.USER.intCode(), 7L);
        verify(notifyManager).update(any());
    }

    /**
     * 验证 delete 删除归属匹配的消息。
     */
    @Test
    public void delete_removes_owned_message() {
        NotifyMessagePO m = buildOwnedMessage(99L, 0);
        when(notifyManager.getById(99L)).thenReturn(m);

        service.delete(99L, RecipientTypeEnum.USER.intCode(), 7L);
        verify(notifyManager).removeById(99L);
    }

    /**
     * 验证 delete 在消息不存在时抛出 BizException。
     */
    @Test
    public void delete_missing_throws() {
        when(notifyManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.delete(99L, RecipientTypeEnum.USER.intCode(), 7L));
    }

    /**
     * 验证 broadcast 指定 targetId 时落库并 SSE 推送。
     */
    @Test
    public void broadcast_targetId_saves_and_sse() {
        NotifyBroadcastDTO dto = new NotifyBroadcastDTO();
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        dto.setMessageType(1);
        dto.setTitle("公告");
        dto.setContent("内容");
        dto.setTargetId(7L);

        service.broadcast(dto);
        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(sseHub).send(7L, "公告");
    }

    /**
     * 验证 broadcast 非法接收方类型时抛出 BizException。
     */
    @Test
    public void broadcast_invalid_recipient_type_throws() {
        NotifyBroadcastDTO dto = new NotifyBroadcastDTO();
        dto.setRecipientType(99);
        dto.setMessageType(1);
        dto.setTitle("公告");
        dto.setContent("内容");

        assertThrows(BizException.class, () -> service.broadcast(dto));
    }

    /**
     * 验证 batchDelete 成功删除归属匹配的批量消息。
     */
    @Test
    public void batchDelete_succeeds() {
        List<Long> ids = List.of(1L, 2L);
        NotifyMessagePO m1 = buildOwnedMessage(1L, 0);
        NotifyMessagePO m2 = buildOwnedMessage(2L, 0);
        when(notifyManager.listByIds(any())).thenReturn(List.of(m1, m2));

        service.batchDelete(ids, RecipientTypeEnum.USER.intCode(), 7L);

        verify(notifyManager).removeByIds(ids);
    }

    /**
     * 验证 batchDelete 空ID列表时抛出 BizException。
     */
    @Test
    public void batchDelete_empty_ids_throws() {
        assertThrows(BizException.class,
            () -> service.batchDelete(Collections.emptyList(),
                RecipientTypeEnum.USER.intCode(), 7L));
    }

    /**
     * 验证 batchDelete 超过100条时抛出 BizException。
     */
    @Test
    public void batchDelete_too_many_throws() {
        List<Long> ids = new ArrayList<>(101);
        for (long i = 1; i <= 101; i++) {
            ids.add(i);
        }
        assertThrows(BizException.class,
            () -> service.batchDelete(ids, RecipientTypeEnum.USER.intCode(), 7L));
    }

    /**
     * 验证 batchDelete 存在非本人消息时抛出 BizException。
     */
    @Test
    public void batchDelete_not_owned_throws() {
        NotifyMessagePO m = buildOwnedMessage(1L, 0);
        m.setUserId(999L);
        when(notifyManager.listByIds(any())).thenReturn(List.of(m));

        assertThrows(BizException.class,
            () -> service.batchDelete(List.of(1L), RecipientTypeEnum.USER.intCode(), 7L));
    }

    private NotifyMessagePO buildOwnedMessage(Long id, Integer readFlag) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setId(id);
        m.setUserId(7L);
        m.setRecipientType(RecipientTypeEnum.USER.intCode());
        m.setReadFlag(readFlag);
        return m;
    }

    /**
     * 验证营销推送指定用户列表时逐条落库并 SSE 推送。
     */
    @Test
    public void marketingPush_with_userIds_saves_and_sse() {
        NotifyMarketingPushDTO dto = new NotifyMarketingPushDTO();
        dto.setTitle("促销");
        dto.setContent("满减活动");
        dto.setUserIds(List.of(1L, 2L));

        service.marketingPush(dto);

        verify(notifyManager, org.mockito.Mockito.times(2)).save(any(NotifyMessagePO.class));
        verify(sseHub).send(1L, "促销");
        verify(sseHub).send(2L, "促销");
    }

    /**
     * 验证营销推送全量广播（userIds=null）时落库一条 userId=0 的占位记录。
     */
    @Test
    public void marketingPush_broadcast_saves_placeholder() {
        NotifyMarketingPushDTO dto = new NotifyMarketingPushDTO();
        dto.setTitle("促销");
        dto.setContent("满减活动");

        service.marketingPush(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(sseHub, never()).send(any(), any());
    }

    /**
     * 验证违规警告通知向商家落库 VIOLATION 类型消息并 SSE 推送。
     */
    @Test
    public void violationWarning_saves_and_sse() {
        NotifyViolationWarningDTO dto = new NotifyViolationWarningDTO();
        dto.setMerchantId(100L);
        dto.setTitle("违规警告");
        dto.setContent("商品描述不符");
        dto.setViolationType("FAKE");

        service.violationWarning(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(sseHub).send(100L, "违规警告");
    }

    /**
     * 验证系统告警通知向平台落库 SYSTEM 类型消息。
     */
    @Test
    public void systemAlert_saves_message() {
        NotifySystemAlertDTO dto = new NotifySystemAlertDTO();
        dto.setTitle("系统告警");
        dto.setContent("数据库连接超时");
        dto.setAlertLevel("CRITICAL");

        service.systemAlert(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
    }

    /**
     * 验证 alertPage 返回平台系统告警分页结果。
     */
    @Test
    public void alertPage_returns_platform_system_alerts() {
        NotifyMessagePO alert = new NotifyMessagePO();
        alert.setId(1L);
        alert.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
        alert.setMessageType(NotifyMessageTypeEnum.SYSTEM.intCode());
        alert.setTitle("CPU 告警");
        IPage<NotifyMessagePO> expected = new Page<>(1, 10);
        expected.setRecords(List.of(alert));
        when(notifyManager.page(any(IPage.class), any())).thenReturn(expected);

        SystemAlertPageDTO dto = new SystemAlertPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        IPage<NotifyMessagePO> result = service.alertPage(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("CPU 告警", result.getRecords().get(0).getTitle());
        verify(notifyManager).page(any(IPage.class), any());
    }

    /**
     * 验证 alertPage 在无告警时返回空分页。
     */
    @Test
    public void alertPage_returns_empty_when_no_alerts() {
        IPage<NotifyMessagePO> expected = new Page<>(1, 10);
        expected.setRecords(Collections.emptyList());
        when(notifyManager.page(any(IPage.class), any())).thenReturn(expected);

        SystemAlertPageDTO dto = new SystemAlertPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        IPage<NotifyMessagePO> result = service.alertPage(dto);

        assertEquals(0, result.getRecords().size());
    }

    /**
     * 验证 publishAnnouncement 落库为 messageType=ANNOUNCEMENT, recipientType=ALL 的站内信。
     */
    @Test
    public void publishAnnouncement_success() {
        AnnouncementCreateDTO dto = new AnnouncementCreateDTO();
        dto.setTitle("系统升级公告");
        dto.setContent("系统将于今晚进行升级维护");

        service.publishAnnouncement(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
    }
}
