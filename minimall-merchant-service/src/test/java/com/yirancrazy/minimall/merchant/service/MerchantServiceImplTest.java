package com.yirancrazy.minimall.merchant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.service.impl.MerchantServiceImpl;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantServiceImpl 单元测试，覆盖资质提交、查询与审核状态流转。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public class MerchantServiceImplTest {

    private MerchantManager manager;
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
        service = new MerchantServiceImpl(manager);
    }

    /**
     * 验证首次提交资质时创建一条 PENDING 记录。
     */
    @Test
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
     * 验证已存在资质时更新并重置为 PENDING。
     */
    @Test
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
     * 验证查询存在资质返回 VO。
     */
    @Test
    public void getQualification_exists() {
        when(manager.getOne(any())).thenReturn(buildPO(1L, 1L, 1));
        MerchantQualificationVO vo = service.getQualification(1L);
        assertEquals(1L, vo.getUserId());
    }

    /**
     * 验证查询不存在资质抛出 MERCHANT_NOT_FOUND。
     */
    @Test
    public void getQualification_missing_throws() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getQualification(999L));
    }

    /**
     * 验证审核通过后状态置为 APPROVED。
     */
    @Test
    public void audit_approve_marks_approved() {
        MerchantPO po = buildPO(1L, 1L, 0);
        when(manager.getById(1L)).thenReturn(po);

        service.audit(1L, true, null);
        assertEquals(Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode()), po.getAuditStatus());
        verify(manager).updateById(po);
    }

    /**
     * 验证审核驳回后状态置为 REJECTED 并记录原因。
     */
    @Test
    public void audit_reject_marks_rejected() {
        MerchantPO po = buildPO(1L, 1L, 0);
        when(manager.getById(1L)).thenReturn(po);

        service.audit(1L, false, "证件不清晰");
        assertEquals(Integer.parseInt(MerchantAuditStatusEnum.REJECTED.getCode()), po.getAuditStatus());
        assertEquals("证件不清晰", po.getAuditReason());
    }

    /**
     * 验证审核不存在商家抛出 MERCHANT_NOT_FOUND。
     */
    @Test
    public void audit_missing_throws() {
        when(manager.getById(1L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.audit(1L, true, null));
    }

    /**
     * 验证已审核资质重复审核抛出 MERCHANT_ALREADY_AUDITED。
     */
    @Test
    public void audit_already_audited_throws() {
        when(manager.getById(1L)).thenReturn(buildPO(1L, 1L, 1));
        assertThrows(BizException.class, () -> service.audit(1L, true, null));
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
}
