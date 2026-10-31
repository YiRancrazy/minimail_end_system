package com.yirancrazy.minimall.notify.service;

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
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.constant.NotifyMessageTypeEnum;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDto;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDto;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDto;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDto;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDto;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;
import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.impl.NotifyServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 覆盖站内通知写入、查询、已读、删除、广播、营销、违规告警、系统告警和公告发布的业务分支与 SSE 副作用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class NotifyServiceImplTest {

  private NotifyManager notifyManager;
  private NotifyPushGateway notifyPushGateway;
  private NotifyServiceImpl service;

  /** 初始化通知表元数据，创建管理器与推送网关 Mock，并构造被测通知服务。 */
  @BeforeEach
  void setUp() {
    // 初始化 TableInfo，便于 LambdaUpdateWrapper 解析 lambda 缓存
    MybatisConfiguration configuration = new MybatisConfiguration();
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
    assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.NotifyMessageMapper");
    TableInfoHelper.initTableInfo(assistant, NotifyMessagePo.class);

    notifyManager = mock(NotifyManager.class);
    notifyPushGateway = mock(NotifyPushGateway.class);
    lenient().when(notifyManager.save(any(NotifyMessagePo.class))).thenReturn(true);
    lenient().when(notifyManager.updateById(any(NotifyMessagePo.class))).thenReturn(true);
    lenient().when(notifyManager.removeById(99L)).thenReturn(true);
    service = new NotifyServiceImpl(notifyManager, notifyPushGateway);
  }

  /** 验证通知保存成功时写库并以用户7和标题触发SSE推送. */
  @Test
  public void testPushPersistsUnreadMessage() {
    boolean ok = service.push(7L, "订单支付成功", "订单 99 已支付");
    assertTrue(ok);
    verify(notifyManager).save(any(NotifyMessagePo.class));
    verify(notifyPushGateway).send(7L, "订单支付成功");
  }

  /** 验证通知保存返回false时push返回false且不触发SSE推送. */
  @Test
  public void testPushReturnsFalseWhenSaveFails() {
    when(notifyManager.save(any(NotifyMessagePo.class))).thenReturn(false);
    boolean ok = service.push(7L, "title", "content");
    assertFalse(ok);
    verify(notifyPushGateway, never()).send(any(), any());
  }

  /** 验证业务唯一键冲突时按重复消息成功处理且不重复推送. */
  @Test
  void testPushDuplicateBizIdIsIdempotent() {
    when(notifyManager.save(any(NotifyMessagePo.class)))
        .thenThrow(new DuplicateKeyException("uk_notify_biz"));

    boolean ok = service.push(7L, "订单支付成功", "订单 99 已支付", "ORDER_PAID:99");

    assertTrue(ok);
    verify(notifyPushGateway, never()).send(any(), any());
  }

  /**
   * 测试目标：验证按用户查询通知返回管理器结果。
   *
   * 前置：管理器 list 返回一条 userId 为 7 的通知。
   * 预期：结果包含该通知且用户编号匹配。
   */
  public void listByUserReturnsMessages() {
    NotifyMessagePo m = new NotifyMessagePo();
    m.setUserId(7L);
    when(notifyManager.list(any(Wrapper.class))).thenReturn(List.of(m));

    NotifyListDto dto = new NotifyListDto();
    dto.setUserId(7L);
    List<NotifyMessagePo> result = service.listByUser(dto);

    assertEquals(1, result.size());
    assertEquals(7L, result.get(0).getUserId());
  }

  /**
   * 测试目标：验证用户没有通知时返回空列表。
   *
   * 前置：管理器 list 返回空集合。
   * 预期：查询结果为空。
   */
  public void listByUserReturnsEmptyWhenNoMessages() {
    when(notifyManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

    NotifyListDto dto = new NotifyListDto();
    dto.setUserId(999L);
    List<NotifyMessagePo> result = service.listByUser(dto);

    assertTrue(result.isEmpty());
  }

  /**
   * 测试目标：验证通知游标查询调用管理器并返回分页对象。
   *
   * 前置：请求 limit 为 10，管理器返回空集合。
   * 预期：返回非空 CursorPageVo，管理器 list 被调用。
   */
  public void pageDelegatesToManager() {
    NotifyListDto dto = new NotifyListDto();
    dto.setLimit(10);
    dto.setUserId(7L);
    dto.setRecipientType(RecipientTypeEnum.USER.intCode());
    List<NotifyMessagePo> records = Collections.emptyList();
    when(notifyManager.list(any(Wrapper.class))).thenReturn(records);

    CursorPageVo<NotifyMessagePo> result = service.page(dto);
    assertNotNull(result);
    verify(notifyManager).list(any(Wrapper.class));
  }

  /**
   * 测试目标：验证通知分页查询包含全端公告占位条件。
   *
   * 前置：用户请求 USER 类型且管理器返回空集合。
   * 预期：查询条件参数包含 userId 0 和接收方类型 ALL。
   */
  public void pageIncludesAnnouncementPlaceholder() {
    NotifyListDto dto = new NotifyListDto();
    dto.setLimit(10);
    dto.setUserId(7L);
    dto.setRecipientType(RecipientTypeEnum.USER.intCode());
    when(notifyManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

    service.page(dto);

    ArgumentCaptor<LambdaQueryWrapper<NotifyMessagePo>> cap =
        ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    verify(notifyManager).list(cap.capture());
    LambdaQueryWrapper<NotifyMessagePo> wrapper = cap.getValue();
    // 嵌套 or 分支的参数在 SQL 生成（getCustomSqlSegment）时才写入 paramNameValuePairs，需先触发
    wrapper.getCustomSqlSegment();
    Map<String, Object> params = wrapper.getParamNameValuePairs();
    assertTrue(params.containsValue(0L));
    assertTrue(params.containsValue(RecipientTypeEnum.ALL.intCode()));
  }

  /**
   * 测试目标：验证没有 targetId 的广播保存为全端占位消息。
   *
   * 前置：接收方类型 ALL，消息类型 ANNOUNCEMENT，无定向用户。
   * 预期：保存 userId 0 和 ALL 类型，且不推送 SSE。
   */
  public void broadcastAllPlaceholderPersists() {
    NotifyBroadcastDto dto = new NotifyBroadcastDto();
    dto.setRecipientType(RecipientTypeEnum.ALL.intCode());
    dto.setMessageType(NotifyMessageTypeEnum.ANNOUNCEMENT.intCode());
    dto.setTitle("全站公告");
    dto.setContent("内容");

    service.broadcast(dto);

    ArgumentCaptor<NotifyMessagePo> cap = ArgumentCaptor.forClass(NotifyMessagePo.class);
    verify(notifyManager).save(cap.capture());
    assertEquals(0L, cap.getValue().getUserId());
    assertEquals(RecipientTypeEnum.ALL.intCode(), cap.getValue().getRecipientType());
    verify(notifyPushGateway, never()).send(any(), any());
  }

  /**
   * 测试目标：验证未读计数直接使用管理器统计结果。
   *
   * 前置：NotifyManager.count 返回 5。
   * 预期：unreadCount 返回 5，并调用 count。
   */
  public void unreadCountDelegatesToManager() {
    when(notifyManager.count(any(Wrapper.class))).thenReturn(5L);

    long count = service.unreadCount(RecipientTypeEnum.USER.intCode(), 7L);
    assertEquals(5L, count);
    verify(notifyManager).count(any(Wrapper.class));
  }

  /**
   * 测试目标：验证未读消息标记已读时更新 readFlag。
   *
   * 前置：消息属于当前用户且 readFlag 为 0。
   * 预期：readFlag 变为 1，并调用 updateById。
   */
  public void markReadUpdatesFlag() {
    NotifyMessagePo m = buildOwnedMessage(99L, 0);
    when(notifyManager.getById(99L)).thenReturn(m);

    service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L);
    assertEquals(1, m.getReadFlag());
    verify(notifyManager).updateById(m);
  }

  /**
   * 测试目标：验证已读消息再次标记时不重复写库。
   *
   * 前置：消息 readFlag 已为 1。
   * 预期：不调用 updateById。
   */
  public void markReadAlreadyReadIdempotent() {
    NotifyMessagePo m = buildOwnedMessage(99L, 1);
    when(notifyManager.getById(99L)).thenReturn(m);

    service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L);
    verify(notifyManager, never()).updateById(any(NotifyMessagePo.class));
  }

  /**
   * 测试目标：验证标记不存在的通知已读时返回业务错误。
   *
   * 前置：getById 返回 null。
   * 预期：抛出 BizException。
   */
  public void markReadMissingThrows() {
    when(notifyManager.getById(99L)).thenReturn(null);
    assertThrows(
        BizException.class, () -> service.markRead(99L, RecipientTypeEnum.USER.intCode(), 7L));
  }

  /**
   * 测试目标：验证全部已读通过条件更新委托给管理器。
   *
   * 前置：当前用户接收方类型为 USER。
   * 预期：调用 NotifyManager.update。
   */
  public void markAllReadDelegatesToUpdate() {
    service.markAllRead(RecipientTypeEnum.USER.intCode(), 7L);
    verify(notifyManager).update(any());
  }

  /**
   * 测试目标：验证用户可删除自己拥有的通知。
   *
   * 前置：消息归属当前用户。
   * 预期：调用 removeById 删除通知。
   */
  public void deleteRemovesOwnedMessage() {
    NotifyMessagePo m = buildOwnedMessage(99L, 0);
    when(notifyManager.getById(99L)).thenReturn(m);

    service.delete(99L, RecipientTypeEnum.USER.intCode(), 7L);
    verify(notifyManager).removeById(99L);
  }

  /**
   * 测试目标：验证删除不存在的通知时返回业务错误。
   *
   * 前置：getById 返回 null。
   * 预期：抛出 BizException。
   */
  public void deleteMissingThrows() {
    when(notifyManager.getById(99L)).thenReturn(null);
    assertThrows(
        BizException.class, () -> service.delete(99L, RecipientTypeEnum.USER.intCode(), 7L));
  }

  /**
   * 测试目标：验证定向广播落库并推送给目标用户。
   *
   * 前置：targetId 为 7，接收方类型和消息类型合法。
   * 预期：保存通知记录，并以用户 7 和公告标题调用 SSE 推送。
   */
  public void broadcastTargetIdSavesAndSse() {
    NotifyBroadcastDto dto = new NotifyBroadcastDto();
    dto.setRecipientType(RecipientTypeEnum.USER.intCode());
    dto.setMessageType(1);
    dto.setTitle("公告");
    dto.setContent("内容");
    dto.setTargetId(7L);

    service.broadcast(dto);
    verify(notifyManager).save(any(NotifyMessagePo.class));
    verify(notifyPushGateway).send(7L, "公告");
  }

  /**
   * 测试目标：验证非法广播接收方类型被拒绝。
   *
   * 前置：recipientType 为 99。
   * 预期：抛出 BizException。
   */
  public void broadcastInvalidRecipientTypeThrows() {
    NotifyBroadcastDto dto = new NotifyBroadcastDto();
    dto.setRecipientType(99);
    dto.setMessageType(1);
    dto.setTitle("公告");
    dto.setContent("内容");

    assertThrows(BizException.class, () -> service.broadcast(dto));
  }

  /**
   * 测试目标：验证批量删除在全部记录归属当前用户时成功。
   *
   * 前置：编号 1、2 对应两条当前用户消息。
   * 预期：调用 removeByIds 删除两条记录。
   */
  public void batchDeleteSucceeds() {
    List<Long> ids = List.of(1L, 2L);
    NotifyMessagePo m1 = buildOwnedMessage(1L, 0);
    NotifyMessagePo m2 = buildOwnedMessage(2L, 0);
    when(notifyManager.listByIds(any())).thenReturn(List.of(m1, m2));

    service.batchDelete(ids, RecipientTypeEnum.USER.intCode(), 7L);

    verify(notifyManager).removeByIds(ids);
  }

  /**
   * 测试目标：验证批量删除空编号集合时拒绝请求。
   *
   * 前置：ids 为空列表。
   * 预期：抛出 BizException。
   */
  public void batchDeleteEmptyIdsThrows() {
    assertThrows(
        BizException.class,
        () -> service.batchDelete(Collections.emptyList(), RecipientTypeEnum.USER.intCode(), 7L));
  }

  /**
   * 测试目标：验证单次批量删除超过 100 条时拒绝请求。
   *
   * 前置：ids 包含 101 个编号。
   * 预期：抛出 BizException。
   */
  public void batchDeleteTooManyThrows() {
    List<Long> ids = new ArrayList<>(101);
    for (long i = 1; i <= 101; i++) {
      ids.add(i);
    }
    assertThrows(
        BizException.class, () -> service.batchDelete(ids, RecipientTypeEnum.USER.intCode(), 7L));
  }

  /**
   * 测试目标：验证批量删除包含他人消息时整体拒绝。
   *
   * 前置：查询到的消息 userId 为 999，当前用户为 7。
   * 预期：抛出 BizException。
   */
  public void batchDeleteNotOwnedThrows() {
    NotifyMessagePo m = buildOwnedMessage(1L, 0);
    m.setUserId(999L);
    when(notifyManager.listByIds(any())).thenReturn(List.of(m));

    assertThrows(
        BizException.class,
        () -> service.batchDelete(List.of(1L), RecipientTypeEnum.USER.intCode(), 7L));
  }

  /** 构造属于用户 7 的指定主键和已读状态通知实体，供测试数据准备复用. */
  private NotifyMessagePo buildOwnedMessage(Long id, Integer readFlag) {
    NotifyMessagePo m = new NotifyMessagePo();
    m.setId(id);
    m.setUserId(7L);
    m.setRecipientType(RecipientTypeEnum.USER.intCode());
    m.setReadFlag(readFlag);
    return m;
  }

  /**
   * 测试目标：验证指定用户的营销推送批量落库并逐人发送 SSE。
   *
   * 前置：目标用户为 1、2。
   * 预期：saveBatch 保存两条记录，单条 save 不调用，两个用户各推送一次。
   */
  public void marketingPushWithUserIdsBatchSavesAndSse() {
    NotifyMarketingPushDto dto = new NotifyMarketingPushDto();
    dto.setTitle("促销");
    dto.setContent("满减活动");
    dto.setUserIds(List.of(1L, 2L));

    service.marketingPush(dto);

    ArgumentCaptor<List<NotifyMessagePo>> cap = ArgumentCaptor.forClass(List.class);
    verify(notifyManager).saveBatch(cap.capture());
    assertEquals(2, cap.getValue().size());
    assertEquals(1L, cap.getValue().get(0).getUserId());
    assertEquals(2L, cap.getValue().get(1).getUserId());
    verify(notifyManager, never()).save(any(NotifyMessagePo.class));
    verify(notifyPushGateway).send(1L, "促销");
    verify(notifyPushGateway).send(2L, "促销");
  }

  /**
   * 测试目标：验证营销推送缺少目标用户时保存全端占位消息。
   *
   * 前置：userIds 为 null。
   * 预期：调用单条 save 保存占位记录，且不推送 SSE。
   */
  public void marketingPushBroadcastSavesPlaceholder() {
    NotifyMarketingPushDto dto = new NotifyMarketingPushDto();
    dto.setTitle("促销");
    dto.setContent("满减活动");

    service.marketingPush(dto);

    verify(notifyManager).save(any(NotifyMessagePo.class));
    verify(notifyPushGateway, never()).send(any(), any());
  }

  /**
   * 测试目标：验证违规警告写入商家通知并实时推送。
   *
   * 前置：商家编号 100，标题和违规类型合法。
   * 预期：保存通知，并以商家 100 和警告标题推送 SSE。
   */
  public void violationWarningSavesAndSse() {
    NotifyViolationWarningDto dto = new NotifyViolationWarningDto();
    dto.setMerchantId(100L);
    dto.setTitle("违规警告");
    dto.setContent("商品描述不符");
    dto.setViolationType("FAKE");

    service.violationWarning(dto);

    verify(notifyManager).save(any(NotifyMessagePo.class));
    verify(notifyPushGateway).send(100L, "违规警告");
  }

  /**
   * 测试目标：验证系统告警写入平台系统消息。
   *
   * 前置：标题、正文和 CRITICAL 级别已提供。
   * 预期：调用 save 保存告警记录。
   */
  public void systemAlertSavesMessage() {
    NotifySystemAlertDto dto = new NotifySystemAlertDto();
    dto.setTitle("系统告警");
    dto.setContent("数据库连接超时");
    dto.setAlertLevel("CRITICAL");

    service.systemAlert(dto);

    verify(notifyManager).save(any(NotifyMessagePo.class));
  }

  /**
   * 测试目标：验证告警分页返回平台系统类型记录。
   *
   * 前置：管理器返回一条 PLATFORM/SYSTEM 告警。
   * 预期：分页包含该记录且标题匹配。
   */
  public void alertPageReturnsPlatformSystemAlerts() {
    NotifyMessagePo alert = new NotifyMessagePo();
    alert.setId(1L);
    alert.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
    alert.setMessageType(NotifyMessageTypeEnum.SYSTEM.intCode());
    alert.setTitle("CPU 告警");
    List<NotifyMessagePo> records = List.of(alert);
    when(notifyManager.list(any(Wrapper.class))).thenReturn(records);

    SystemAlertPageDto dto = new SystemAlertPageDto();
    dto.setLimit(10);
    CursorPageVo<NotifyMessagePo> result = service.alertPage(dto);

    assertEquals(1, result.getRecords().size());
    assertEquals("CPU 告警", result.getRecords().get(0).getTitle());
    verify(notifyManager).list(any(Wrapper.class));
  }

  /**
   * 测试目标：验证没有系统告警时返回空分页。
   *
   * 前置：管理器 list 返回空集合。
   * 预期：分页记录数为 0。
   */
  public void alertPageReturnsEmptyWhenNoAlerts() {
    List<NotifyMessagePo> records = Collections.emptyList();
    when(notifyManager.list(any(Wrapper.class))).thenReturn(records);

    SystemAlertPageDto dto = new SystemAlertPageDto();
    dto.setLimit(10);
    CursorPageVo<NotifyMessagePo> result = service.alertPage(dto);

    assertEquals(0, result.getRecords().size());
  }

  /**
   * 测试目标：验证公告发布调用通知保存。
   *
   * 前置：公告标题和正文已提供。
   * 预期：NotifyManager.save 被调用一次。
   */
  public void publishAnnouncementSuccess() {
    AnnouncementCreateDto dto = new AnnouncementCreateDto();
    dto.setTitle("系统升级公告");
    dto.setContent("系统将于今晚进行升级维护");

    service.publishAnnouncement(dto);

    verify(notifyManager).save(any(NotifyMessagePo.class));
  }
}
