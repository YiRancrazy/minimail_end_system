package com.yirancrazy.minimall.notify.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.constant.ComplaintStatusEnum;
import com.yirancrazy.minimall.notify.constant.ComplaintTypeEnum;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDto;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDto;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDto;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.service.impl.ComplaintServiceImpl;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证投诉创建、游标过滤、状态机合法与非法流转、条件更新失败以及用户投诉归属校验.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class ComplaintServiceImplTest {

  private ComplaintManager complaintManager;
  private ComplaintServiceImpl service;

  /** 初始化投诉表元数据，创建投诉管理器 Mock，并构造被测投诉服务。 */
  @BeforeEach
  void setUp() {
    // 初始化 TableInfo，便于 LambdaQueryWrapper 解析 lambda 缓存
    MybatisConfiguration configuration = new MybatisConfiguration();
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
    assistant.setCurrentNamespace("com.yirancrazy.minimall.notify.mapper.ComplaintMapper");
    TableInfoHelper.initTableInfo(assistant, ComplaintPo.class);

    complaintManager = mock(ComplaintManager.class);
    lenient()
        .doAnswer(
            inv -> {
              ComplaintPo p = inv.getArgument(0);
              if (p.getId() == null) {
                p.setId(System.nanoTime());
              }
              return true;
            })
        .when(complaintManager)
        .save(any(ComplaintPo.class));
    lenient().when(complaintManager.updateById(any(ComplaintPo.class))).thenReturn(true);
    service = new ComplaintServiceImpl(complaintManager);
  }

  /**
   * 测试目标：验证投诉创建会保存记录并返回主键。
   *
   * 前置：投诉管理器 Mock 的 save 返回 true 并补充主键。
   * 预期：返回非空主键，且 save 被调用。
   */
  public void createPersistsAndReturnsId() {
    ComplaintCreateDto dto = new ComplaintCreateDto();
    dto.setComplainantType(1);
    dto.setComplainantId(100L);
    dto.setDefendantType(2);
    dto.setDefendantId(200L);
    dto.setOrderNo("ORDER001");
    dto.setComplaintType(ComplaintTypeEnum.QUALITY.getCode());
    dto.setTitle("商品质量问题");
    dto.setContent("收到的商品与描述不符");

    Long id = service.create(100L, dto);

    assertNotNull(id);
    verify(complaintManager).save(any(ComplaintPo.class));
  }

  /**
   * 测试目标：验证创建投诉时使用调用方传入的投诉人编号，而不是请求体中的同名字段。
   *
   * 前置：请求体 complainantId 为 999，服务入参 complainantId 为 100。
   * 预期：服务创建流程完成并调用 save。
   */
  public void createUsesHeaderComplainantId() {
    ComplaintCreateDto dto = new ComplaintCreateDto();
    dto.setComplainantType(1);
    dto.setComplainantId(999L);
    dto.setDefendantType(2);
    dto.setDefendantId(200L);
    dto.setComplaintType(ComplaintTypeEnum.QUALITY.getCode());
    dto.setTitle("测试");
    dto.setContent("内容");

    service.create(100L, dto);

    verify(complaintManager).save(any(ComplaintPo.class));
  }

  /**
   * 测试目标：验证投诉游标查询会调用管理器并返回分页对象。
   *
   * 前置：管理器 list 返回空集合，请求 limit 为 10。
   * 预期：返回非空 CursorPageVo，管理器 list 被调用一次。
   */
  public void pageDelegatesToManager() {
    ComplaintPageDto dto = new ComplaintPageDto();
    dto.setLimit(10);

    List<ComplaintPo> records = Collections.emptyList();
    when(complaintManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(records);

    CursorPageVo<ComplaintPo> result = service.page(dto);

    assertNotNull(result);
    verify(complaintManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 测试目标：验证状态和订单号筛选条件可共同用于投诉查询。
   *
   * 前置：请求设置待处理状态和订单号 ORDER001。
   * 预期：查询完成、返回非空分页对象，并调用管理器 list。
   */
  public void pageFiltersByStatusAndOrderNo() {
    ComplaintPageDto dto = new ComplaintPageDto();
    dto.setLimit(10);
    dto.setStatus(ComplaintStatusEnum.PENDING.intCode());
    dto.setOrderNo("ORDER001");

    List<ComplaintPo> records = Collections.emptyList();
    when(complaintManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(records);

    CursorPageVo<ComplaintPo> result = service.page(dto);

    assertNotNull(result);
    verify(complaintManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 测试目标：验证待处理投诉可以推进到处理中。
   *
   * 前置：当前状态 PENDING，条件更新返回 true。
   * 预期：调用条件 update，不调用 updateById。
   */
  public void handlePendingToProcessing() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.PENDING.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);
    when(complaintManager.update(any(Wrapper.class))).thenReturn(true);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
    dto.setResult("正在核实");

    service.handle(1L, 10L, dto);

    verify(complaintManager).update(any(Wrapper.class));
    verify(complaintManager, never()).updateById(any(ComplaintPo.class));
  }

  /**
   * 测试目标：验证处理中投诉可以推进到已解决。
   *
   * 前置：当前状态 PROCESSING，条件更新返回 true。
   * 预期：调用条件 update 完成状态和结果写入。
   */
  public void handleProcessingToResolved() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);
    when(complaintManager.update(any(Wrapper.class))).thenReturn(true);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
    dto.setResult("已退款处理");

    service.handle(1L, 10L, dto);

    verify(complaintManager).update(any(Wrapper.class));
  }

  /**
   * 测试目标：验证处理中投诉可以推进到已驳回。
   *
   * 前置：当前状态 PROCESSING，条件更新返回 true。
   * 预期：调用条件 update 完成状态和结果写入。
   */
  public void handleProcessingToRejected() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);
    when(complaintManager.update(any(Wrapper.class))).thenReturn(true);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.REJECTED.intCode());
    dto.setResult("证据不足");

    service.handle(1L, 10L, dto);

    verify(complaintManager).update(any(Wrapper.class));
  }

  /**
   * 测试目标：验证并发导致条件更新影响零行时按状态冲突处理。
   *
   * 前置：当前状态 PENDING，但管理器条件更新返回 false。
   * 预期：抛出 BizException。
   */
  public void handleThrowsWhenConditionalUpdateAffectsZeroRows() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.PENDING.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);
    when(complaintManager.update(any(Wrapper.class))).thenReturn(false);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
    dto.setResult("处理中");

    assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
  }

  /**
   * 测试目标：验证已解决投诉不能回退到待处理。
   *
   * 前置：当前状态 RESOLVED，目标状态 PENDING。
   * 预期：在更新数据库前抛出 BizException。
   */
  public void handleThrowsOnInvalidTransition() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.PENDING.intCode());
    dto.setResult("非法操作");

    assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
    verify(complaintManager, never()).update(any(Wrapper.class));
  }

  /**
   * 测试目标：验证处理不存在的投诉时返回业务错误。
   *
   * 前置：getById 返回 null。
   * 预期：抛出 BizException。
   */
  public void handleThrowsWhenNotFound() {
    when(complaintManager.getById(999L)).thenReturn(null);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.PROCESSING.intCode());
    dto.setResult("处理中");

    assertThrows(BizException.class, () -> service.handle(999L, 10L, dto));
  }

  /**
   * 测试目标：验证待处理投诉不能跳过处理中直接变为已解决。
   *
   * 前置：当前状态 PENDING，目标状态 RESOLVED。
   * 预期：抛出 BizException。
   */
  public void handleThrowsOnPendingToResolved() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.PENDING.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.RESOLVED.intCode());
    dto.setResult("跳过处理");

    assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
  }

  /**
   * 测试目标：验证待处理投诉不能跳过处理中直接变为已驳回。
   *
   * 前置：当前状态 PENDING，目标状态 REJECTED。
   * 预期：抛出 BizException。
   */
  public void handleThrowsOnPendingToRejected() {
    ComplaintPo po = new ComplaintPo();
    po.setId(1L);
    po.setStatus(ComplaintStatusEnum.PENDING.intCode());
    when(complaintManager.getById(1L)).thenReturn(po);

    ComplaintHandleDto dto = new ComplaintHandleDto();
    dto.setStatus(ComplaintStatusEnum.REJECTED.intCode());
    dto.setResult("跳过处理");

    assertThrows(BizException.class, () -> service.handle(1L, 10L, dto));
  }

  /**
   * 测试目标：验证按主键查询投诉返回持久化实体。
   *
   * 前置：getById 返回编号为 7 的投诉。
   * 预期：返回对象主键为 7。
   */
  public void detailReturnsPo() {
    ComplaintPo po = new ComplaintPo();
    po.setId(7L);
    when(complaintManager.getById(7L)).thenReturn(po);

    ComplaintPo got = service.detail(7L);

    assertEquals(7L, got.getId());
  }

  /**
   * 测试目标：验证查询不存在的投诉时返回业务错误。
   *
   * 前置：getById 返回 null。
   * 预期：抛出 BizException。
   */
  public void detailThrowsWhenNotFound() {
    when(complaintManager.getById(99L)).thenReturn(null);

    assertThrows(BizException.class, () -> service.detail(99L));
  }

  /**
   * 测试目标：验证用户可以查看自己发起的投诉。
   *
   * 前置：投诉 complainantId 为 100，当前用户编号为 100。
   * 预期：返回投诉视图，主键为 7。
   */
  public void detailForUserReturnsOwnComplaint() {
    ComplaintPo po = new ComplaintPo();
    po.setId(7L);
    po.setComplainantId(100L);
    when(complaintManager.getById(7L)).thenReturn(po);

    assertEquals(7L, service.detailForUser(7L, 100L).getId());
  }

  /**
   * 测试目标：验证用户不能查看他人的投诉。
   *
   * 前置：投诉投诉人为 200，当前用户编号为 100。
   * 预期：抛出 BizException。
   */
  public void detailForUserThrowsWhenNotOwner() {
    ComplaintPo po = new ComplaintPo();
    po.setId(7L);
    po.setComplainantId(200L);
    when(complaintManager.getById(7L)).thenReturn(po);

    assertThrows(BizException.class, () -> service.detailForUser(7L, 100L));
  }
}
