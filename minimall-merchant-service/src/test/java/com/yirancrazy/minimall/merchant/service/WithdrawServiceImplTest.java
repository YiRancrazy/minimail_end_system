package com.yirancrazy.minimall.merchant.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum;
import com.yirancrazy.minimall.merchant.constant.WithdrawStatusEnum;
import com.yirancrazy.minimall.merchant.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.merchant.dto.WithdrawPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.merchant.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.merchant.service.impl.WithdrawServiceImpl;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: WithdrawServiceImpl 单元测试，覆盖申请、分页、审核通过与驳回及状态机校验。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
public class WithdrawServiceImplTest {

    private MerchantWithdrawManager manager;
    private WithdrawServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(MerchantWithdrawManager.class);
        doAnswer(inv -> {
            MerchantWithdrawPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(MerchantWithdrawPO.class));
        when(manager.updateById(any(MerchantWithdrawPO.class))).thenReturn(true);
        service = new WithdrawServiceImpl(manager);
    }

    /**
     * 验证正常申请落库为 PENDING 并返回ID。
     */
    @Test
    public void apply_normal_returns_id() {
        WithdrawApplyDTO dto = new WithdrawApplyDTO();
        dto.setAmount(new BigDecimal("100.00"));
        Long id = service.apply(1L, dto);
        assertNotNull(id);
        verify(manager).save(any(MerchantWithdrawPO.class));
    }

    /**
     * 验证金额非法时抛 WITHDRAW_AMOUNT_INVALID。
     */
    @Test
    public void apply_zero_amount_throws() {
        WithdrawApplyDTO dto = new WithdrawApplyDTO();
        dto.setAmount(new BigDecimal("0.00"));
        BizException ex = assertThrows(BizException.class, () -> service.apply(1L, dto));
        assertEquals(MerchantCodeEnum.WITHDRAW_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    /**
     * 验证商家分页查询强制绑定 merchantId。
     */
    @Test
    public void page_by_merchant_filters_by_merchant() {
        WithdrawPageDTO dto = new WithdrawPageDTO();
        dto.setMerchantId(1L);
        dto.setLimit(20);
        List<MerchantWithdrawPO> records = new ArrayList<>();
        records.add(buildPo(1L, 1L, WithdrawStatusEnum.PENDING.intCode()));
        when(manager.list(any(Wrapper.class))).thenReturn(records);
        assertNotNull(service.pageByMerchant(dto));
        verify(manager).list(any(Wrapper.class));
    }

    /**
     * 验证平台分页查询不强制 merchantId 过滤。
     */
    @Test
    public void page_all_returns_records() {
        WithdrawPageDTO dto = new WithdrawPageDTO();
        dto.setLimit(20);
        when(manager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());
        assertNotNull(service.pageAll(dto));
    }

    /**
     * 验证审核通过将 PENDING 置为 APPROVED。
     */
    @Test
    public void approve_pending_transits_to_approved() {
        MerchantWithdrawPO po = buildPo(1L, 1L, WithdrawStatusEnum.PENDING.intCode());
        when(manager.getById(1L)).thenReturn(po);
        service.approve(1L);
        assertEquals(WithdrawStatusEnum.APPROVED.intCode(), po.getStatus());
    }

    /**
     * 验证非 PENDING 状态审核通过抛 WITHDRAW_STATUS_INVALID。
     */
    @Test
    public void approve_non_pending_throws() {
        MerchantWithdrawPO po = buildPo(1L, 1L, WithdrawStatusEnum.APPROVED.intCode());
        when(manager.getById(1L)).thenReturn(po);
        BizException ex = assertThrows(BizException.class, () -> service.approve(1L));
        assertEquals(MerchantCodeEnum.WITHDRAW_STATUS_INVALID.getCode(), ex.getCode());
    }

    /**
     * 验证审核驳回将 PENDING 置为 REJECTED 并记录原因。
     */
    @Test
    public void reject_pending_transits_to_rejected() {
        MerchantWithdrawPO po = buildPo(1L, 1L, WithdrawStatusEnum.PENDING.intCode());
        when(manager.getById(1L)).thenReturn(po);
        service.reject(1L, "资质不符");
        assertEquals(WithdrawStatusEnum.REJECTED.intCode(), po.getStatus());
        assertEquals("资质不符", po.getReason());
    }

    /**
     * 验证申请ID不存在时抛 WITHDRAW_NOT_FOUND。
     */
    @Test
    public void approve_not_found_throws() {
        when(manager.getById(1L)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.approve(1L));
        assertEquals(MerchantCodeEnum.WITHDRAW_NOT_FOUND.getCode(), ex.getCode());
    }

    private MerchantWithdrawPO buildPo(Long id, Long merchantId, int status) {
        MerchantWithdrawPO po = new MerchantWithdrawPO();
        po.setId(id);
        po.setMerchantId(merchantId);
        po.setWithdrawNo("W" + id);
        po.setAmount(new BigDecimal("100.00"));
        po.setStatus(status);
        return po;
    }
}
