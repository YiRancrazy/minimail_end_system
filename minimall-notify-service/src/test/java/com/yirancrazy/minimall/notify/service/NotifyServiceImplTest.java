package com.yirancrazy.minimall.notify.service;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.impl.NotifyServiceImpl;

/**
 * NotifyServiceImpl 单元测试，覆盖消息持久化与按用户查询的正常、失败、边界路径。
 */
public class NotifyServiceImplTest {

    private NotifyManager notifyManager;
    private NotifyServiceImpl service;

    @BeforeEach
    void setUp() {
        notifyManager = mock(NotifyManager.class);
        lenient().when(notifyManager.save(any(NotifyMessagePO.class))).thenReturn(true);
        service = new NotifyServiceImpl(notifyManager);
    }

    /**
     * 验证 push 成功时返回 true 并将消息标记为未读。
     */
    @Test
    public void push_persists_unread_message() {
        boolean ok = service.push(7L, "订单支付成功", "订单 99 已支付");
        assertTrue(ok);
        verify(notifyManager).save(any(NotifyMessagePO.class));
    }

    /**
     * 验证 save 失败时 push 返回 false。
     */
    @Test
    public void push_returns_false_when_save_fails() {
        when(notifyManager.save(any(NotifyMessagePO.class))).thenReturn(false);
        boolean ok = service.push(7L, "title", "content");
        assertFalse(ok);
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
}
