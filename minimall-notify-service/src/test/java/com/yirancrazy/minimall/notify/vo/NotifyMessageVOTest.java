package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMessageVO 单元测试，覆盖 from 转换方法的字段映射与可选字段空值场景。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public class NotifyMessageVOTest {

    /**
     * 验证 from 方法正确映射 NotifyMessagePO 的所有字段。
     */
    @Test
    public void from_mapsAllFields() {
        NotifyMessagePO po = new NotifyMessagePO();
        po.setId(1L);
        po.setUserId(7L);
        po.setRecipientType(1);
        po.setMessageType(2);
        po.setTitle("订单发货通知");
        po.setContent("您的订单已发货");
        po.setReadFlag(0);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 10, 0, 0);
        po.setCreateTime(now);

        NotifyMessageVO vo = NotifyMessageVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(7L, vo.getUserId());
        assertEquals(1, vo.getRecipientType());
        assertEquals("LOGISTICS", vo.getType());
        assertEquals("订单发货通知", vo.getTitle());
        assertEquals("您的订单已发货", vo.getContent());
        assertEquals("UNREAD", vo.getStatus());
        assertEquals(now, vo.getCreateTime());
    }

    /**
     * 验证 from 方法在可选字段为 null 时正确映射已设置的字段并保留空值。
     */
    @Test
    public void from_nullOptionalFields() {
        NotifyMessagePO po = new NotifyMessagePO();
        po.setId(1L);
        po.setUserId(7L);
        po.setRecipientType(1);
        po.setMessageType(2);
        po.setReadFlag(0);
        // title、content、senderId、bizId、createTime 留空

        NotifyMessageVO vo = NotifyMessageVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(7L, vo.getUserId());
        assertEquals(1, vo.getRecipientType());
        assertEquals("LOGISTICS", vo.getType());
        assertEquals("UNREAD", vo.getStatus());
        assertNull(vo.getTitle());
        assertNull(vo.getContent());
        assertNull(vo.getCreateTime());
    }
}
