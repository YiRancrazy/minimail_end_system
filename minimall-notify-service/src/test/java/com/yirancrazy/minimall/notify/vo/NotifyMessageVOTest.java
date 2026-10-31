package com.yirancrazy.minimall.notify.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证通知视图对完整字段的映射、消息类型别名转换以及可选字段为空时的兼容行为.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class NotifyMessageVoTest {

  /**
   * 测试目标：验证通知实体的完整字段会映射到展示视图。
   *
   * 前置：消息类型为 2，readFlag 为 0，并设置标题、正文和创建时间。
   * 预期：类型为 LOGISTICS、状态为 UNREAD，其余字段与实体一致。
   */
  public void fromMapsAllFields() {
    NotifyMessagePo po = new NotifyMessagePo();
    po.setId(1L);
    po.setUserId(7L);
    po.setRecipientType(1);
    po.setMessageType(2);
    po.setTitle("订单发货通知");
    po.setContent("您的订单已发货");
    po.setReadFlag(0);
    LocalDateTime now = LocalDateTime.of(2026, 8, 4, 10, 0, 0);
    po.setCreateTime(now);

    NotifyMessageVo vo = NotifyMessageVo.from(po);

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
   * 测试目标：验证通知可选展示字段为空时转换不报错。
   *
   * 前置：标题、正文和创建时间为 null，消息类型为 2，readFlag 为 0。
   * 预期：类型为 LOGISTICS、状态为 UNREAD，可选字段保持 null。
   */
  public void fromNullOptionalFields() {
    NotifyMessagePo po = new NotifyMessagePo();
    po.setId(1L);
    po.setUserId(7L);
    po.setRecipientType(1);
    po.setMessageType(2);
    po.setReadFlag(0);
    // title、content、senderId、bizId、createTime 留空

    NotifyMessageVo vo = NotifyMessageVo.from(po);

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
