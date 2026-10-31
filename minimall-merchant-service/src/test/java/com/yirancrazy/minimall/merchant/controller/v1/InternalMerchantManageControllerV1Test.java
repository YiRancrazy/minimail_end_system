package com.yirancrazy.minimall.merchant.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部商家管理接口测试定义；使用 MerchantService mock 覆盖分页、详情和审核委派。当前仅有 setUp 标注 @BeforeEach，其余场景方法缺少 @Test，暂不参与自动执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class InternalMerchantManageControllerV1Test {

  private MerchantService merchantService;
  private InternalMerchantManageControllerV1 controller;

  /**
   * 初始化商家服务 mock 和内部商家管理控制器。
   */
  @BeforeEach
  void setUp() {
    merchantService = mock(MerchantService.class);
    controller = new InternalMerchantManageControllerV1(merchantService);
  }

  /**
   * 验证分页参数会转换为商家筛选条件并返回分页结果。
   *
   * 前置条件：服务返回一条商家记录，查询包含 limit、keyword 和 status。关键步骤：调用 controller.page。预期：业务码 00000，记录数、商家名称与 mock 数据一致。当前方法缺少 @Test。
   */
  public void pageDelegatesWithFilters() {
    MerchantManageVo vo = buildMerchant(1L, 100L, "shopA");
    lenient()
        .when(merchantService.page(any(com.yirancrazy.minimall.merchant.dto.MerchantPageDto.class)))
        .thenReturn(CursorPageVo.of(java.util.List.of(vo), 20, MerchantManageVo::getMerchantId));

    InternalPageQuery query = new InternalPageQuery();
    query.setLimit(20);
    query.setKeyword("shop");
    query.setStatus(1);
    Result<CursorPageVo<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo>> r =
        controller.page(query);

    assertEquals("00000", r.getCode());
    assertEquals(1, r.getData().getRecords().size());
    assertEquals("shopA", r.getData().getRecords().get(0).getMerchantName());
  }

  /**
   * 验证无匹配商家时返回空游标分页。
   *
   * 前置条件：服务返回空分页。关键步骤：调用 controller.page。预期：业务码成功且 records 为空。当前方法缺少 @Test。
   */
  public void pageReturnsEmptyWhenNoRecords() {
    when(merchantService.page(any(com.yirancrazy.minimall.merchant.dto.MerchantPageDto.class)))
        .thenReturn(CursorPageVo.empty(20));

    InternalPageQuery query = new InternalPageQuery();
    Result<CursorPageVo<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo>> r =
        controller.page(query);

    assertEquals(0, r.getData().getRecords().size());
  }

  /**
   * 验证内部详情接口返回服务层商家视图。
   *
   * 前置条件：服务按 ID 1 返回 shopA。关键步骤：调用 controller.detail(1)。预期：响应数据商家名称为 shopA。当前方法缺少 @Test。
   */
  public void detailReturnsVo() {
    when(merchantService.detail(1L)).thenReturn(buildMerchant(1L, 100L, "shopA"));

    Result<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo> r = controller.detail(1L);

    assertEquals("shopA", r.getData().getMerchantName());
  }

  /**
   * 验证商家不存在时内部详情接口传播业务异常。
   *
   * 前置条件：服务按 ID 99 抛出 MERCHANT_NOT_FOUND。关键步骤：调用 controller.detail(99)。预期：抛出 BizException。当前方法缺少 @Test。
   */
  public void detailThrowsWhenNotFound() {
    when(merchantService.detail(99L))
        .thenThrow(
            new BizException(
                com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum.MERCHANT_NOT_FOUND));

    assertThrows(BizException.class, () -> controller.detail(99L));
  }

  /**
   * 验证审核通过请求原样委派给服务层。
   *
   * 关键步骤：调用 controller.audit(1, true, null)。预期：MerchantService.audit 收到相同参数。当前方法缺少 @Test。
   */
  public void auditApprovedDelegatesToService() {
    controller.audit(1L, true, null);
    verify(merchantService).audit(1L, true, null);
  }

  /**
   * 验证内部接口将驳回原因原样委派给服务层。
   *
   * 关键步骤：以 false 和空白原因调用 controller.audit。预期：控制器不提前处理，服务收到原值，具体校验由服务层负责。当前方法缺少 @Test。
   */
  public void auditRejectDelegatesToService() {
    controller.audit(1L, false, "  ");
    verify(merchantService).audit(1L, false, "  ");
  }

  /**
   * 验证重复审核异常会从服务层向调用方传播。
   *
   * 前置条件：mock 服务抛出 MERCHANT_ALREADY_AUDITED。关键步骤：调用 controller.audit。预期：抛出 BizException。当前方法缺少 @Test。
   */
  public void auditConflictPropagatesFromService() {
    org.mockito.Mockito.doThrow(
            new BizException(
                com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum
                    .MERCHANT_ALREADY_AUDITED))
        .when(merchantService)
        .audit(1L, false, "原因");

    assertThrows(BizException.class, () -> controller.audit(1L, false, "原因"));
  }

  /**
   * 构造内部管理分页测试使用的商家视图。
   *
   * @param merchantId 商家主键
   * @param userId 商家用户 ID
   * @param name 商家名称
   * @return 使用固定证照号和审核状态的测试视图。
   */
  private MerchantManageVo buildMerchant(Long merchantId, Long userId, String name) {
    return new MerchantManageVo(merchantId, userId, name, "L1", null, 1, null, null, null);
  }
}
