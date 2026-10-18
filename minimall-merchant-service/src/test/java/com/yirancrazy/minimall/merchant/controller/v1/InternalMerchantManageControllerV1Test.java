package com.yirancrazy.minimall.merchant.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.service.MerchantService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalMerchantManageControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class InternalMerchantManageControllerV1Test {

    private MerchantService merchantService;
    private InternalMerchantManageControllerV1 controller;

    @BeforeEach
    void setUp() {
        merchantService = mock(MerchantService.class);
        controller = new InternalMerchantManageControllerV1(merchantService);
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_with_filters() {
        MerchantManageVO vo = buildMerchant(1L, 100L, "shopA");
        lenient().when(merchantService.page(any(com.yirancrazy.minimall.merchant.dto.MerchantPageDTO.class)))
                .thenReturn(CursorPageVO.of(java.util.List.of(vo), 20, MerchantManageVO::getMerchantId));

        InternalPageQuery query = new InternalPageQuery();
        query.setLimit(20);
        query.setKeyword("shop");
        query.setStatus(1);
        Result<CursorPageVO<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO>> r = controller.page(query);

        assertEquals("00000", r.getCode());
        assertEquals(1, r.getData().getRecords().size());
        assertEquals("shopA", r.getData().getRecords().get(0).getMerchantName());
    }


    /**
     * 分页查询数据。
     */
    public void page_returns_empty_when_no_records() {
        when(merchantService.page(any(com.yirancrazy.minimall.merchant.dto.MerchantPageDTO.class)))
                .thenReturn(CursorPageVO.empty(20));

        InternalPageQuery query = new InternalPageQuery();
        Result<CursorPageVO<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO>> r = controller.page(query);

        assertEquals(0, r.getData().getRecords().size());
    }


    /**
     * 执行 detail_returns_vo 方法。
     */
    public void detail_returns_vo() {
        when(merchantService.detail(1L)).thenReturn(buildMerchant(1L, 100L, "shopA"));

        Result<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO> r = controller.detail(1L);

        assertEquals("shopA", r.getData().getMerchantName());
    }


    /**
     * 执行 detail_throws_when_not_found 方法。
     */
    public void detail_throws_when_not_found() {
        when(merchantService.detail(99L)).thenThrow(new BizException(
                com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum.MERCHANT_NOT_FOUND));

        assertThrows(BizException.class, () -> controller.detail(99L));
    }


    /**
     * 执行 audit_approved_delegates_to_service 方法。
     */
    public void audit_approved_delegates_to_service() {
        controller.audit(1L, true, null);
        verify(merchantService).audit(1L, true, null);
    }


    /**
     * 执行 audit_reject_delegates_to_service 方法。
     */
    public void audit_reject_delegates_to_service() {
        controller.audit(1L, false, "  ");
        verify(merchantService).audit(1L, false, "  ");
    }


    /**
     * 执行 audit_conflict_propagates_from_service 方法。
     */
    public void audit_conflict_propagates_from_service() {
        org.mockito.Mockito.doThrow(new BizException(
                com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum.MERCHANT_ALREADY_AUDITED))
            .when(merchantService).audit(1L, false, "原因");

        assertThrows(BizException.class, () -> controller.audit(1L, false, "原因"));
    }

    private MerchantManageVO buildMerchant(Long merchantId, Long userId, String name) {
        return new MerchantManageVO(merchantId, userId, name, "L1", null, 1,
                null, null, null);
    }
}
