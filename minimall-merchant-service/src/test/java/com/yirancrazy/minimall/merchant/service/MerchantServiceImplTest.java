package com.yirancrazy.minimall.merchant.service;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.service.impl.MerchantServiceImpl;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVO;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class MerchantServiceImplTest {

    private MerchantManager manager;
    private MinioUtil minioUtil;
    private MerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(MerchantManager.class);
        doAnswer(inv -> {
            MerchantPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(MerchantPO.class));
        when(manager.updateById(any(MerchantPO.class))).thenReturn(true);
        minioUtil = mock(MinioUtil.class);
        service = new MerchantServiceImpl(manager, minioUtil);
    }


    /**
     * 执行 submit_new_creates_pending 方法。
     */
    public void submit_new_creates_pending() {
        when(manager.getOne(any())).thenReturn(null);
        QualificationSubmitDTO dto = new QualificationSubmitDTO();
        dto.setMerchantName("薄荷旗舰店");
        dto.setLicenseNo("91110000MA00ABCDEF");

        MerchantQualificationVO vo = service.submitQualification(1L, dto);
        assertEquals(0, vo.getAuditStatus());
        assertEquals("薄荷旗舰店", vo.getMerchantName());
        verify(manager).save(any(MerchantPO.class));
    }


    /**
     * 执行 submit_existing_updates_pending 方法。
     */
    public void submit_existing_updates_pending() {
        MerchantPO existing = buildPO(1L, 1L, 1);
        when(manager.getOne(any())).thenReturn(existing);

        QualificationSubmitDTO dto = new QualificationSubmitDTO();
        dto.setMerchantName("新店名");
        dto.setLicenseNo("NEW123");
        service.submitQualification(1L, dto);

        assertEquals(0, existing.getAuditStatus());
        assertEquals("新店名", existing.getMerchantName());
        verify(manager).updateById(existing);
    }


    /**
     * 执行 submit_carries_sensitive_fields 方法。
     */
    public void submit_carries_sensitive_fields() {
        when(manager.getOne(any())).thenReturn(null);
        QualificationSubmitDTO dto = new QualificationSubmitDTO();
        dto.setMerchantName("测试商家");
        dto.setLicenseNo("L123");
        dto.setLegalPerson("张三");
        dto.setLegalPhone("13800001111");
        dto.setIdCardNo("110101199001011234");
        dto.setBusinessLicenseNo("BZ123456");
        dto.setBankAccount("6222000011112222");

        service.submitQualification(1L, dto);

        org.mockito.ArgumentCaptor<MerchantPO> captor = org.mockito.ArgumentCaptor.forClass(MerchantPO.class);
        verify(manager).save(captor.capture());
        MerchantPO po = captor.getValue();
        assertEquals("张三", po.getLegalPersonEnc());
        assertEquals("13800001111", po.getLegalPhoneEnc());
        assertEquals("110101199001011234", po.getIdCardNoEnc());
        assertEquals("BZ123456", po.getBusinessLicenseNoEnc());
        assertEquals("6222000011112222", po.getBankAccountEnc());
    }


    /**
     * 执行 submit_persists_and_resolves_license_image 方法。
     */
    public void submit_persists_and_resolves_license_image() {
        when(manager.getOne(any())).thenReturn(null);
        when(minioUtil.resolvePublicUrl("lic/abc.png")).thenReturn("http://minio/mall-files/lic/abc.png?token");
        QualificationSubmitDTO dto = new QualificationSubmitDTO();
        dto.setMerchantName("测试商家");
        dto.setLicenseNo("L123");
        dto.setLicenseImageUrl("lic/abc.png");

        com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO vo = service.submitQualification(1L, dto);

        org.mockito.ArgumentCaptor<MerchantPO> captor = org.mockito.ArgumentCaptor.forClass(MerchantPO.class);
        verify(manager).save(captor.capture());
        assertEquals("lic/abc.png", captor.getValue().getLicenseImageUrl());
        assertEquals("http://minio/mall-files/lic/abc.png?token", vo.getLicenseImageUrl());
    }


    /**
     * 获取相关数据。
     */
    public void getQualification_masks_sensitive_fields() {
        MerchantPO po = buildPO(1L, 1L, 1);
        po.setIdCardNoEnc("110101199001011234");
        po.setLegalPhoneEnc("13800001111");
        po.setBankAccountEnc("6222000011112222");
        when(manager.getOne(any())).thenReturn(po);

        MerchantQualificationVO vo = service.getQualification(1L);

        assertTrue(vo.getIdCardNoMasked().contains("****"));
        assertTrue(vo.getLegalPhoneMasked().contains("****"));
        assertTrue(vo.getBankAccountMasked().contains("****"));
    }


    /**
     * 获取相关数据。
     */
    public void getQualification_exists() {
        when(manager.getOne(any())).thenReturn(buildPO(1L, 1L, 1));
        MerchantQualificationVO vo = service.getQualification(1L);
        assertEquals(1L, vo.getUserId());
    }


    /**
     * 获取相关数据。
     */
    public void getQualification_missing_throws() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getQualification(999L));
    }


    /**
     * 执行 audit_approve_marks_approved 方法。
     */
    public void audit_approve_marks_approved() {
        MerchantPO po = buildPO(1L, 1L, 0);
        when(manager.getById(1L)).thenReturn(po);

        service.audit(1L, true, null);
        assertEquals(Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode()), po.getAuditStatus());
        verify(manager).updateById(po);
    }


    /**
     * 执行 audit_reject_marks_rejected 方法。
     */
    public void audit_reject_marks_rejected() {
        MerchantPO po = buildPO(1L, 1L, 0);
        when(manager.getById(1L)).thenReturn(po);

        service.audit(1L, false, "证件不清晰");
        assertEquals(Integer.parseInt(MerchantAuditStatusEnum.REJECTED.getCode()), po.getAuditStatus());
        assertEquals("证件不清晰", po.getAuditReason());
    }


    /**
     * 执行 audit_missing_throws 方法。
     */
    public void audit_missing_throws() {
        when(manager.getById(1L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.audit(1L, true, null));
    }


    /**
     * 执行 audit_already_audited_throws 方法。
     */
    public void audit_already_audited_throws() {
        when(manager.getById(1L)).thenReturn(buildPO(1L, 1L, 1));
        assertThrows(BizException.class, () -> service.audit(1L, true, null));
    }


    /**
     * 执行 audit_reject_without_reason_throws 方法。
     */
    public void audit_reject_without_reason_throws() {
        when(manager.getById(1L)).thenReturn(buildPO(1L, 1L, 0));
        assertThrows(BizException.class, () -> service.audit(1L, false, null));
        org.mockito.Mockito.verify(manager, org.mockito.Mockito.never())
                .updateById(any(MerchantPO.class));
    }


    /**
     * 执行 audit_null_status_throws_already_audited 方法。
     */
    public void audit_null_status_throws_already_audited() {
        MerchantPO po = buildPO(1L, 1L, 0);
        po.setAuditStatus(null);
        when(manager.getById(1L)).thenReturn(po);
        assertThrows(BizException.class, () -> service.audit(1L, true, null));
    }


    /**
     * 执行 audit_update_conflict_throws_already_audited 方法。
     */
    public void audit_update_conflict_throws_already_audited() {
        MerchantPO po = buildPO(1L, 1L, 0);
        when(manager.getById(1L)).thenReturn(po);
        when(manager.updateById(any(MerchantPO.class))).thenReturn(false);
        assertThrows(BizException.class, () -> service.audit(1L, true, null));
    }


    /**
     * 执行 audit_approve_clears_stale_reason 方法。
     */
    public void audit_approve_clears_stale_reason() {
        MerchantPO po = buildPO(1L, 1L, 0);
        po.setAuditReason("历史驳回原因");
        when(manager.getById(1L)).thenReturn(po);

        service.audit(1L, true, null);
        assertEquals(null, po.getAuditReason());
        verify(manager).updateById(po);
    }


    /**
     * 获取相关数据。
     */
    public void getMerchantInfo_success() {
        MerchantPO po = buildPO(1L, 10L, 1);
        when(manager.getOne(any())).thenReturn(po);

        MerchantInfoVO vo = service.getMerchantInfo(10L);

        assertEquals(10L, vo.getMerchantId());
        assertEquals("测试店", vo.getShopName());
        assertEquals("LIC123", vo.getLicenseNo());
        assertEquals(1, vo.getQualificationStatus());
    }


    /**
     * 获取相关数据。
     */
    public void getMerchantInfo_notFound_throws() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getMerchantInfo(999L));
    }


    /**
     * 查询列表数据。
     */
    public void listAuditLog_success() {
        LocalDateTime auditAt = LocalDateTime.of(2026, 8, 4, 12, 0);
        MerchantPO po = buildPO(1L, 10L, 2);
        po.setAuditReason("证件不清晰");
        po.setAuditAt(auditAt);
        when(manager.getOne(any())).thenReturn(po);

        List<MerchantAuditLogVO> list = service.listAuditLog(10L);

        assertEquals(1, list.size());
        MerchantAuditLogVO vo = list.get(0);
        assertEquals(1L, vo.getId());
        assertEquals("QUALIFICATION", vo.getAuditType());
        assertEquals(10L, vo.getTargetId());
        assertEquals(2, vo.getDecision());
        assertEquals("证件不清晰", vo.getReason());
        assertEquals(auditAt, vo.getAuditAt());
    }


    /**
     * 查询列表数据。
     */
    public void listAuditLog_empty() {
        when(manager.getOne(any())).thenReturn(null);
        List<MerchantAuditLogVO> list = service.listAuditLog(999L);
        assertTrue(list.isEmpty());
    }

    private MerchantPO buildPO(Long id, Long userId, int auditStatus) {
        MerchantPO po = new MerchantPO();
        po.setId(id);
        po.setUserId(userId);
        po.setMerchantName("测试店");
        po.setLicenseNo("LIC123");
        po.setAuditStatus(auditStatus);
        return po;
    }


    /**
     * 分页查询数据。
     */
    public void page_returns_empty() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.Collections.emptyList());

        com.yirancrazy.minimall.merchant.dto.MerchantPageDTO dto =
                new com.yirancrazy.minimall.merchant.dto.MerchantPageDTO();
        dto.setLimit(20);
        com.yirancrazy.minimall.common.result.CursorPageVO<MerchantManageVO> page = service.page(dto);

        assertTrue(page.getRecords().isEmpty());
        assertEquals(false, page.isHasMore());
    }


    /**
     * 分页查询数据。
     */
    public void page_filters_combined() {
        MerchantPO a = buildPO(1L, 100L, 1);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(java.util.List.of(a));

        com.yirancrazy.minimall.merchant.dto.MerchantPageDTO dto =
                new com.yirancrazy.minimall.merchant.dto.MerchantPageDTO();
        dto.setLimit(20);
        dto.setKeyword("shop");
        dto.setAuditStatus(1);
        com.yirancrazy.minimall.common.result.CursorPageVO<MerchantManageVO> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 detail_returns_manage_vo 方法。
     */
    public void detail_returns_manage_vo() {
        when(manager.getById(1L)).thenReturn(buildPO(1L, 100L, 1));

        MerchantManageVO got = service.detail(1L);
        assertEquals(100L, got.getUserId());
    }


    /**
     * 执行 detail_throws_when_not_found 方法。
     */
    public void detail_throws_when_not_found() {
        when(manager.getById(99L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
