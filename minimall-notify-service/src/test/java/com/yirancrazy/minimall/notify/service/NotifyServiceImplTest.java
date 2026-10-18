package com.yirancrazy.minimall.notify.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
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
import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.impl.NotifyServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class NotifyServiceImplTest {

    private NotifyManager notifyManager;
    private NotifyPushGateway notifyPushGateway;
    private NotifyServiceImpl service;

    @BeforeEach
    void setUp() {
        // 初始化 TableInfo，便于 LambdaUpdateWrapper 解析 lambda 缓存
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.NotifyMessageMapper");
        TableInfoHelper.initTableInfo(assistant, NotifyMessagePO.class);

        notifyManager = mock(NotifyManager.class);
        notifyPushGateway = mock(NotifyPushGateway.class);
        lenient().when(notifyManager.save(any(NotifyMessagePO.class))).thenReturn(true);
        lenient().when(notifyManager.updateById(any(NotifyMessagePO.class))).thenReturn(true);
        lenient().when(notifyManager.removeById(99L)).thenReturn(true);
        service = new NotifyServiceImpl(notifyManager, notifyPushGateway);
    }


    /**
     * 执行 push_persists_unread_message 方法。
     */
    public void push_persists_unread_message() {
        boolean ok = service.push(7L, "订单支付成功", "订单 99 已支付");
        assertTrue(ok);
        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway).send(7L, "订单支付成功");
    }


    /**
     * 执行 push_returns_false_when_save_fails 方法。
     */
    public void push_returns_false_when_save_fails() {
        when(notifyManager.save(any(NotifyMessagePO.class))).thenReturn(false);
        boolean ok = service.push(7L, "title", "content");
        assertFalse(ok);
        verify(notifyPushGateway, never()).send(any(), any());
    }


    /**
     * 查询列表数据。
     */
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
     * 查询列表数据。
     */
    public void listByUser_returns_empty_when_no_messages() {
        when(notifyManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        NotifyListDTO dto = new NotifyListDTO();
        dto.setUserId(999L);
        List<NotifyMessagePO> result = service.listByUser(dto);

        assertTrue(result.isEmpty());
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_to_manager() {
        NotifyListDTO dto = new NotifyListDTO();
        dto.setLimit(10);
        dto.setUserId(7L);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        List<NotifyMessagePO> records = Collections.emptyList();
        when(notifyManager.list(any(Wrapper.class))).thenReturn(records);

        CursorPageVO<NotifyMessagePO> result = service.page(dto);
        assertNotNull(result);
        verify(notifyManager).list(any(Wrapper.class));
    }


    /**
     * 分页查询数据。
     */
    public void page_includes_announcement_placeholder() {
        NotifyListDTO dto = new NotifyListDTO();
        dto.setLimit(10);
        dto.setUserId(7L);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        when(notifyManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        service.page(dto);

        ArgumentCaptor<LambdaQueryWrapper<NotifyMessagePO>> cap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(notifyManager).list(cap.capture());
        LambdaQueryWrapper<NotifyMessagePO> wrapper = cap.getValue();
        // 嵌套 or 分支的参数在 SQL 生成（getCustomSqlSegment）时才写入 paramNameValuePairs，需先触发
        wrapper.getCustomSqlSegment();
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue(0L));
        assertTrue(params.containsValue(RecipientTypeEnum.ALL.intCode()));
    }


    /**
     * 执行 broadcast_all_placeholder_persists 方法。
     */
    public void broadcast_all_placeholder_persists() {
        NotifyBroadcastDTO dto = new NotifyBroadcastDTO();
        dto.setRecipientType(RecipientTypeEnum.ALL.intCode());
        dto.setMessageType(NotifyMessageTypeEnum.ANNOUNCEMENT.intCode());
        dto.setTitle("全站公告");
        dto.setContent("内容");

        service.broadcast(dto);

        ArgumentCaptor<NotifyMessagePO> cap = ArgumentCaptor.forClass(NotifyMessagePO.class);
        verify(notifyManager).save(cap.capture());
        assertEquals(0L, cap.getValue().getUserId());
        assertEquals(RecipientTypeEnum.ALL.intCode(), cap.getValue().getRecipientType());
        verify(notifyPushGateway, never()).send(any(), any());
    }


    /**
     * 执行 unreadCount_delegates_to_manager 方法。
     */
    public void unreadCount_delegates_to_manager() {
        when(notifyManager.count(any(Wrapper.class))).thenReturn(5L);

        long count = service.unreadCount(RecipientTypeEnum.USER.intCode(), 7L);
        assertEquals(5L, count);
        verify(notifyManager).count(any(Wrapper.class));
    }


    /**
     * 执行 markRead_updates_flag 方法。
     */
    public void markRead_updates_flag() {
        NotifyMessagePO m = buildOwnedMessage(99L, 0);
        when(notifyManager.getById(99L)).thenReturn(m);

        service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L);
        assertEquals(1, m.getReadFlag());
        verify(notifyManager).updateById(m);
    }


    /**
     * 执行 markRead_already_read_idempotent 方法。
     */
    public void markRead_already_read_idempotent() {
        NotifyMessagePO m = buildOwnedMessage(99L, 1);
        when(notifyManager.getById(99L)).thenReturn(m);

        service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L);
        verify(notifyManager, never()).updateById(any(NotifyMessagePO.class));
    }


    /**
     * 执行 markRead_missing_throws 方法。
     */
    public void markRead_missing_throws() {
        when(notifyManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L));
    }


    /**
     * 执行 markAllRead_delegates_to_update 方法。
     */
    public void markAllRead_delegates_to_update() {
        service.markAllRead(RecipientTypeEnum.USER.intCode(), 7L);
        verify(notifyManager).update(any());
    }


    /**
     * 删除业务数据。
     */
    public void delete_removes_owned_message() {
        NotifyMessagePO m = buildOwnedMessage(99L, 0);
        when(notifyManager.getById(99L)).thenReturn(m);

        service.delete(99L, RecipientTypeEnum.USER.intCode(), 7L);
        verify(notifyManager).removeById(99L);
    }


    /**
     * 删除业务数据。
     */
    public void delete_missing_throws() {
        when(notifyManager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.delete(99L, RecipientTypeEnum.USER.intCode(), 7L));
    }


    /**
     * 执行 broadcast_targetId_saves_and_sse 方法。
     */
    public void broadcast_targetId_saves_and_sse() {
        NotifyBroadcastDTO dto = new NotifyBroadcastDTO();
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        dto.setMessageType(1);
        dto.setTitle("公告");
        dto.setContent("内容");
        dto.setTargetId(7L);

        service.broadcast(dto);
        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway).send(7L, "公告");
    }


    /**
     * 执行 broadcast_invalid_recipient_type_throws 方法。
     */
    public void broadcast_invalid_recipient_type_throws() {
        NotifyBroadcastDTO dto = new NotifyBroadcastDTO();
        dto.setRecipientType(99);
        dto.setMessageType(1);
        dto.setTitle("公告");
        dto.setContent("内容");

        assertThrows(BizException.class, () -> service.broadcast(dto));
    }


    /**
     * 执行 batchDelete_succeeds 方法。
     */
    public void batchDelete_succeeds() {
        List<Long> ids = List.of(1L, 2L);
        NotifyMessagePO m1 = buildOwnedMessage(1L, 0);
        NotifyMessagePO m2 = buildOwnedMessage(2L, 0);
        when(notifyManager.listByIds(any())).thenReturn(List.of(m1, m2));

        service.batchDelete(ids, RecipientTypeEnum.USER.intCode(), 7L);

        verify(notifyManager).removeByIds(ids);
    }


    /**
     * 执行 batchDelete_empty_ids_throws 方法。
     */
    public void batchDelete_empty_ids_throws() {
        assertThrows(BizException.class,
            () -> service.batchDelete(Collections.emptyList(),
                RecipientTypeEnum.USER.intCode(), 7L));
    }


    /**
     * 执行 batchDelete_too_many_throws 方法。
     */
    public void batchDelete_too_many_throws() {
        List<Long> ids = new ArrayList<>(101);
        for (long i = 1; i <= 101; i++) {
            ids.add(i);
        }
        assertThrows(BizException.class,
            () -> service.batchDelete(ids, RecipientTypeEnum.USER.intCode(), 7L));
    }


    /**
     * 执行 batchDelete_not_owned_throws 方法。
     */
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
     * 执行 marketingPush_with_userIds_batch_saves_and_sse 方法。
     */
    public void marketingPush_with_userIds_batch_saves_and_sse() {
        NotifyMarketingPushDTO dto = new NotifyMarketingPushDTO();
        dto.setTitle("促销");
        dto.setContent("满减活动");
        dto.setUserIds(List.of(1L, 2L));

        service.marketingPush(dto);

        ArgumentCaptor<List<NotifyMessagePO>> cap = ArgumentCaptor.forClass(List.class);
        verify(notifyManager).saveBatch(cap.capture());
        assertEquals(2, cap.getValue().size());
        assertEquals(1L, cap.getValue().get(0).getUserId());
        assertEquals(2L, cap.getValue().get(1).getUserId());
        verify(notifyManager, never()).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway).send(1L, "促销");
        verify(notifyPushGateway).send(2L, "促销");
    }


    /**
     * 执行 marketingPush_broadcast_saves_placeholder 方法。
     */
    public void marketingPush_broadcast_saves_placeholder() {
        NotifyMarketingPushDTO dto = new NotifyMarketingPushDTO();
        dto.setTitle("促销");
        dto.setContent("满减活动");

        service.marketingPush(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway, never()).send(any(), any());
    }


    /**
     * 执行 violationWarning_saves_and_sse 方法。
     */
    public void violationWarning_saves_and_sse() {
        NotifyViolationWarningDTO dto = new NotifyViolationWarningDTO();
        dto.setMerchantId(100L);
        dto.setTitle("违规警告");
        dto.setContent("商品描述不符");
        dto.setViolationType("FAKE");

        service.violationWarning(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway).send(100L, "违规警告");
    }


    /**
     * 执行 systemAlert_saves_message 方法。
     */
    public void systemAlert_saves_message() {
        NotifySystemAlertDTO dto = new NotifySystemAlertDTO();
        dto.setTitle("系统告警");
        dto.setContent("数据库连接超时");
        dto.setAlertLevel("CRITICAL");

        service.systemAlert(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
    }


    /**
     * 执行 alertPage_returns_platform_system_alerts 方法。
     */
    public void alertPage_returns_platform_system_alerts() {
        NotifyMessagePO alert = new NotifyMessagePO();
        alert.setId(1L);
        alert.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
        alert.setMessageType(NotifyMessageTypeEnum.SYSTEM.intCode());
        alert.setTitle("CPU 告警");
        List<NotifyMessagePO> records = List.of(alert);
        when(notifyManager.list(any(Wrapper.class))).thenReturn(records);

        SystemAlertPageDTO dto = new SystemAlertPageDTO();
        dto.setLimit(10);
        CursorPageVO<NotifyMessagePO> result = service.alertPage(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("CPU 告警", result.getRecords().get(0).getTitle());
        verify(notifyManager).list(any(Wrapper.class));
    }


    /**
     * 执行 alertPage_returns_empty_when_no_alerts 方法。
     */
    public void alertPage_returns_empty_when_no_alerts() {
        List<NotifyMessagePO> records = Collections.emptyList();
        when(notifyManager.list(any(Wrapper.class))).thenReturn(records);

        SystemAlertPageDTO dto = new SystemAlertPageDTO();
        dto.setLimit(10);
        CursorPageVO<NotifyMessagePO> result = service.alertPage(dto);

        assertEquals(0, result.getRecords().size());
    }


    /**
     * 执行 publishAnnouncement_success 方法。
     */
    public void publishAnnouncement_success() {
        AnnouncementCreateDTO dto = new AnnouncementCreateDTO();
        dto.setTitle("系统升级公告");
        dto.setContent("系统将于今晚进行升级维护");

        service.publishAnnouncement(dto);

        verify(notifyManager).save(any(NotifyMessagePO.class));
    }
}
