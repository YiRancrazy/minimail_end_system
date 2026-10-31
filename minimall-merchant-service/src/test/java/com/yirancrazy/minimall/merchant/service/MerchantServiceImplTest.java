package com.yirancrazy.minimall.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDto;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.service.impl.MerchantServiceImpl;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVo;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVo;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVo;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantServiceImpl 场景测试定义；使用 MerchantManager 和 MinioUtil mock 覆盖资质提交、脱敏、审核、查询、分页和详情。当前仅 setUp 标注 @BeforeEach，场景方法缺少 @Test，暂不参与自动执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class MerchantServiceImplTest {

  private MerchantManager manager;
  private MinioUtil minioUtil;
  private MerchantServiceImpl service;

  /**
   * 初始化商家 Manager、Minio 工具和服务实例。
   *
   * Manager.save 的回调会为新增记录回填 ID；updateById 默认返回 true。
   */
  @BeforeEach
  void setUp() {
    manager = mock(MerchantManager.class);
    doAnswer(
            inv -> {
              MerchantPo p = inv.getArgument(0);
              if (p.getId() == null) {
                p.setId(System.nanoTime());
              }
              return true;
            })
        .when(manager)
        .save(any(MerchantPo.class));
    when(manager.updateById(any(MerchantPo.class))).thenReturn(true);
    minioUtil = mock(MinioUtil.class);
    service = new MerchantServiceImpl(manager, minioUtil);
  }

  /**
   * 验证首次提交资质会创建待审核记录。
   *
   * 前置条件：按商家用户 ID 查询无记录。关键步骤：提交名称和证照号后捕获 save 调用。预期：返回审核状态 0、商家名称正确，并保存一条新记录。
   */
  public void submitNewCreatesPending() {
    when(manager.getOne(any())).thenReturn(null);
    QualificationSubmitDto dto = new QualificationSubmitDto();
    dto.setMerchantName("薄荷旗舰店");
    dto.setLicenseNo("91110000MA00ABCDEF");

    MerchantQualificationVo vo = service.submitQualification(1L, dto);
    assertEquals(0, vo.getAuditStatus());
    assertEquals("薄荷旗舰店", vo.getMerchantName());
    verify(manager).save(any(MerchantPo.class));
  }

  /**
   * 验证重复提交资质会更新已有记录并恢复待审核。
   *
   * 前置条件：已有已通过资质记录。关键步骤：提交新名称和证照号。预期：原对象状态变为 0、名称更新，并调用 updateById。
   */
  public void submitExistingUpdatesPending() {
    MerchantPo existing = buildPo(1L, 1L, 1);
    when(manager.getOne(any())).thenReturn(existing);

    QualificationSubmitDto dto = new QualificationSubmitDto();
    dto.setMerchantName("新店名");
    dto.setLicenseNo("NEW123");
    service.submitQualification(1L, dto);

    assertEquals(0, existing.getAuditStatus());
    assertEquals("新店名", existing.getMerchantName());
    verify(manager).updateById(existing);
  }

  /**
   * 验证资质提交会把敏感材料写入对应 Enc 字段。
   *
   * 前置条件：首次提交且 DTO 包含法人、手机号、身份证、营业执照号和银行卡。关键步骤：捕获待保存对象。预期：五个 Enc 字段在落库前保存 DTO 原值，等待类型处理器加密。
   */
  public void submitCarriesSensitiveFields() {
    when(manager.getOne(any())).thenReturn(null);
    QualificationSubmitDto dto = new QualificationSubmitDto();
    dto.setMerchantName("测试商家");
    dto.setLicenseNo("L123");
    dto.setLegalPerson("张三");
    dto.setLegalPhone("13800001111");
    dto.setIdCardNo("110101199001011234");
    dto.setBusinessLicenseNo("BZ123456");
    dto.setBankAccount("6222000011112222");

    service.submitQualification(1L, dto);

    org.mockito.ArgumentCaptor<MerchantPo> captor =
        org.mockito.ArgumentCaptor.forClass(MerchantPo.class);
    verify(manager).save(captor.capture());
    MerchantPo po = captor.getValue();
    assertEquals("张三", po.getLegalPersonEnc());
    assertEquals("13800001111", po.getLegalPhoneEnc());
    assertEquals("110101199001011234", po.getIdCardNoEnc());
    assertEquals("BZ123456", po.getBusinessLicenseNoEnc());
    assertEquals("6222000011112222", po.getBankAccountEnc());
  }

  /**
   * 验证证照对象键按原值落库并在返回视图中解析为公开 URL。
   *
   * 前置条件：MinioUtil 为对象键返回带 token 的 URL。关键步骤：提交资质并捕获持久化对象和返回视图。预期：数据库字段保留对象键，VO 返回解析后的 URL。
   */
  public void submitPersistsAndResolvesLicenseImage() {
    when(manager.getOne(any())).thenReturn(null);
    when(minioUtil.resolvePublicUrl("lic/abc.png"))
        .thenReturn("http://minio/mall-files/lic/abc.png?token");
    QualificationSubmitDto dto = new QualificationSubmitDto();
    dto.setMerchantName("测试商家");
    dto.setLicenseNo("L123");
    dto.setLicenseImageUrl("lic/abc.png");

    com.yirancrazy.minimall.merchant.vo.MerchantQualificationVo vo =
        service.submitQualification(1L, dto);

    org.mockito.ArgumentCaptor<MerchantPo> captor =
        org.mockito.ArgumentCaptor.forClass(MerchantPo.class);
    verify(manager).save(captor.capture());
    assertEquals("lic/abc.png", captor.getValue().getLicenseImageUrl());
    assertEquals("http://minio/mall-files/lic/abc.png?token", vo.getLicenseImageUrl());
  }

  /**
   * 验证资质详情不会直接返回手机号、身份证和银行卡明文。
   *
   * 前置条件：持久化对象包含三类敏感原值。关键步骤：查询资质详情。预期：三个 masked 字段均包含掩码字符。
   */
  public void getQualificationMasksSensitiveFields() {
    MerchantPo po = buildPo(1L, 1L, 1);
    po.setIdCardNoEnc("110101199001011234");
    po.setLegalPhoneEnc("13800001111");
    po.setBankAccountEnc("6222000011112222");
    when(manager.getOne(any())).thenReturn(po);

    MerchantQualificationVo vo = service.getQualification(1L);

    assertTrue(vo.getIdCardNoMasked().contains("****"));
    assertTrue(vo.getLegalPhoneMasked().contains("****"));
    assertTrue(vo.getBankAccountMasked().contains("****"));
  }

  /**
   * 验证已有资质可以被成功查询。
   *
   * 前置条件：按商家用户 ID 返回一条记录。关键步骤：查询资质详情。预期：返回视图的 userId 与持久化对象一致。
   */
  public void getQualificationExists() {
    when(manager.getOne(any())).thenReturn(buildPo(1L, 1L, 1));
    MerchantQualificationVo vo = service.getQualification(1L);
    assertEquals(1L, vo.getUserId());
  }

  /**
   * 验证查询不存在资质时抛出业务异常。
   *
   * 前置条件：按商家用户 ID 查询结果为空。关键步骤：调用 getQualification。预期：抛出 BizException。
   */
  public void getQualificationMissingThrows() {
    when(manager.getOne(any())).thenReturn(null);
    assertThrows(BizException.class, () -> service.getQualification(999L));
  }

  /**
   * 验证待审核资质可以通过审核。
   *
   * 前置条件：记录状态为待审核。关键步骤：调用 audit 并传 approved=true。预期：状态变为已通过并调用 updateById。
   */
  public void auditApproveMarksApproved() {
    MerchantPo po = buildPo(1L, 1L, 0);
    when(manager.getById(1L)).thenReturn(po);

    service.audit(1L, true, null);
    assertEquals(Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode()), po.getAuditStatus());
    verify(manager).updateById(po);
  }

  /**
   * 验证待审核资质可以携带原因驳回。
   *
   * 前置条件：记录状态为待审核。关键步骤：以 approved=false 和原因调用审核。预期：状态变为已驳回，审核原因保存为本次输入。
   */
  public void auditRejectMarksRejected() {
    MerchantPo po = buildPo(1L, 1L, 0);
    when(manager.getById(1L)).thenReturn(po);

    service.audit(1L, false, "证件不清晰");
    assertEquals(Integer.parseInt(MerchantAuditStatusEnum.REJECTED.getCode()), po.getAuditStatus());
    assertEquals("证件不清晰", po.getAuditReason());
  }

  /**
   * 验证审核不存在资质时抛出业务异常。
   *
   * 前置条件：按商家记录主键查询结果为空。关键步骤：调用 audit。预期：抛出 BizException。
   */
  public void auditMissingThrows() {
    when(manager.getById(1L)).thenReturn(null);
    assertThrows(BizException.class, () -> service.audit(1L, true, null));
  }

  /**
   * 验证已审核资质不能重复审核。
   *
   * 前置条件：记录状态为已通过。关键步骤：再次调用审核。预期：抛出 BizException。
   */
  public void auditAlreadyAuditedThrows() {
    when(manager.getById(1L)).thenReturn(buildPo(1L, 1L, 1));
    assertThrows(BizException.class, () -> service.audit(1L, true, null));
  }

  /**
   * 验证驳回原因缺失时拒绝审核。
   *
   * 前置条件：记录为待审核，approved=false 且 reason 为空。关键步骤：调用 audit。预期：抛出 BizException，且不调用 updateById。
   */
  public void auditRejectWithoutReasonThrows() {
    when(manager.getById(1L)).thenReturn(buildPo(1L, 1L, 0));
    assertThrows(BizException.class, () -> service.audit(1L, false, null));
    org.mockito.Mockito.verify(manager, org.mockito.Mockito.never())
        .updateById(any(MerchantPo.class));
  }

  /**
   * 验证脏数据中的空审核状态不会被当作待审核。
   *
   * 前置条件：记录 auditStatus=null。关键步骤：调用审核。预期：按已审核状态处理并抛出 BizException。
   */
  public void auditNullStatusThrowsAlreadyAudited() {
    MerchantPo po = buildPo(1L, 1L, 0);
    po.setAuditStatus(null);
    when(manager.getById(1L)).thenReturn(po);
    assertThrows(BizException.class, () -> service.audit(1L, true, null));
  }

  /**
   * 验证乐观锁更新失败会转换为重复审核异常。
   *
   * 前置条件：记录为待审核，但 manager.updateById 返回 false。关键步骤：调用审核。预期：抛出 MERCHANT_ALREADY_AUDITED 对应业务异常，不静默成功。
   */
  public void auditUpdateConflictThrowsAlreadyAudited() {
    MerchantPo po = buildPo(1L, 1L, 0);
    when(manager.getById(1L)).thenReturn(po);
    when(manager.updateById(any(MerchantPo.class))).thenReturn(false);
    assertThrows(BizException.class, () -> service.audit(1L, true, null));
  }

  /**
   * 验证审核通过会清空历史驳回原因。
   *
   * 前置条件：待审核记录仍保留旧驳回原因。关键步骤：执行通过审核。预期：auditReason 变为 null 并完成更新。
   */
  public void auditApproveClearsStaleReason() {
    MerchantPo po = buildPo(1L, 1L, 0);
    po.setAuditReason("历史驳回原因");
    when(manager.getById(1L)).thenReturn(po);

    service.audit(1L, true, null);
    assertEquals(null, po.getAuditReason());
    verify(manager).updateById(po);
  }

  /**
   * 验证商家概览字段来自持久化资质记录。
   *
   * 前置条件：按用户 ID 10 查询到资质记录。关键步骤：调用 getMerchantInfo。预期：merchantId、店铺名称、证照号和资质状态映射正确。
   */
  public void getMerchantInfoSuccess() {
    MerchantPo po = buildPo(1L, 10L, 1);
    when(manager.getOne(any())).thenReturn(po);

    MerchantInfoVo vo = service.getMerchantInfo(10L);

    assertEquals(10L, vo.getMerchantId());
    assertEquals("测试店", vo.getShopName());
    assertEquals("LIC123", vo.getLicenseNo());
    assertEquals(1, vo.getQualificationStatus());
  }

  /**
   * 验证商家资质不存在时概览查询失败。
   *
   * 前置条件：查询结果为空。关键步骤：调用 getMerchantInfo(999)。预期：抛出 BizException。
   */
  public void getMerchantInfoNotFoundThrows() {
    when(manager.getOne(any())).thenReturn(null);
    assertThrows(BizException.class, () -> service.getMerchantInfo(999L));
  }

  /**
   * 验证已有资质会生成一条审核记录视图。
   *
   * 前置条件：商家记录为已驳回并带原因和审核时间。关键步骤：调用 listAuditLog。预期：返回一条 QUALIFICATION 记录，目标、决策、原因和时间映射正确。
   */
  public void listAuditLogSuccess() {
    LocalDateTime auditAt = LocalDateTime.of(2026, 8, 4, 12, 0);
    MerchantPo po = buildPo(1L, 10L, 2);
    po.setAuditReason("证件不清晰");
    po.setAuditAt(auditAt);
    when(manager.getOne(any())).thenReturn(po);

    List<MerchantAuditLogVo> list = service.listAuditLog(10L);

    assertEquals(1, list.size());
    MerchantAuditLogVo vo = list.get(0);
    assertEquals(1L, vo.getId());
    assertEquals("QUALIFICATION", vo.getAuditType());
    assertEquals(10L, vo.getTargetId());
    assertEquals(2, vo.getDecision());
    assertEquals("证件不清晰", vo.getReason());
    assertEquals(auditAt, vo.getAuditAt());
  }

  /**
   * 验证商家资质不存在时审核记录查询返回空列表。
   *
   * 前置条件：按用户 ID 999 查询不到资质。关键步骤：调用 listAuditLog。预期：返回空列表而非异常。
   */
  public void listAuditLogEmpty() {
    when(manager.getOne(any())).thenReturn(null);
    List<MerchantAuditLogVo> list = service.listAuditLog(999L);
    assertTrue(list.isEmpty());
  }

  /**
   * 构造商家服务测试使用的持久化对象。
   *
   * @param id 商家记录主键
   * @param userId 商家用户 ID
   * @param auditStatus 审核状态 code
   * @return 带固定名称和证照号的商家对象。
   */
  private MerchantPo buildPo(Long id, Long userId, int auditStatus) {
    MerchantPo po = new MerchantPo();
    po.setId(id);
    po.setUserId(userId);
    po.setMerchantName("测试店");
    po.setLicenseNo("LIC123");
    po.setAuditStatus(auditStatus);
    return po;
  }

  /**
   * 验证 Manager 无记录时返回空游标分页。
   *
   * 前置条件：Manager.list 返回空集合，limit=20。关键步骤：调用 service.page。预期：records 为空且 hasMore=false。
   */
  public void pageReturnsEmpty() {
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.Collections.emptyList());

    com.yirancrazy.minimall.merchant.dto.MerchantPageDto dto =
        new com.yirancrazy.minimall.merchant.dto.MerchantPageDto();
    dto.setLimit(20);
    com.yirancrazy.minimall.common.result.CursorPageVo<MerchantManageVo> page = service.page(dto);

    assertTrue(page.getRecords().isEmpty());
    assertEquals(false, page.isHasMore());
  }

  /**
   * 验证分页会组合关键字和审核状态筛选。
   *
   * 前置条件：Manager.list 返回一条记录，DTO 带 keyword 和 auditStatus。关键步骤：调用 service.page。预期：结果成功映射，并调用 Manager.list 一次。
   */
  public void pageFiltersCombined() {
    MerchantPo a = buildPo(1L, 100L, 1);
    when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
        .thenReturn(java.util.List.of(a));

    com.yirancrazy.minimall.merchant.dto.MerchantPageDto dto =
        new com.yirancrazy.minimall.merchant.dto.MerchantPageDto();
    dto.setLimit(20);
    dto.setKeyword("shop");
    dto.setAuditStatus(1);
    com.yirancrazy.minimall.common.result.CursorPageVo<MerchantManageVo> page = service.page(dto);

    assertEquals(1, page.getRecords().size());
    verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
  }

  /**
   * 验证内部详情按商家主键返回管理视图。
   *
   * 前置条件：Manager.getById(1) 返回商家记录。关键步骤：调用 service.detail(1)。预期：返回视图中的 userId 为 100。
   */
  public void detailReturnsManageVo() {
    when(manager.getById(1L)).thenReturn(buildPo(1L, 100L, 1));

    MerchantManageVo got = service.detail(1L);
    assertEquals(100L, got.getUserId());
  }

  /**
   * 验证内部详情查询不到记录时抛出业务异常。
   *
   * 前置条件：Manager.getById(99) 返回 null。关键步骤：调用 service.detail(99)。预期：抛出 BizException。
   */
  public void detailThrowsWhenNotFound() {
    when(manager.getById(99L)).thenReturn(null);

    assertThrows(BizException.class, () -> service.detail(99L));
  }
}
