package com.yirancrazy.minimall.merchant.controller.v1;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.service.MerchantService;

/**
 * InternalMerchantManageControllerV1 单元测试。
 */
public class InternalMerchantManageControllerV1Test {

    private MerchantService merchantService;
    private InternalMerchantManageControllerV1 controller;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.merchant.mapper.MerchantMapper");
        TableInfoHelper.initTableInfo(assistant, MerchantPO.class);

        merchantService = mock(MerchantService.class);
        controller = new InternalMerchantManageControllerV1(merchantService);
    }

    /**
     * 验证 page 委托 service 并按 keyword/auditStatus 过滤。
     */
    @Test
    public void page_delegates_with_filters() {
        MerchantPO p = buildMerchant(1L, 100L, "shopA");
        lenient().when(merchantService.page(any(com.yirancrazy.minimall.merchant.dto.MerchantPageDTO.class)))
                .thenReturn(CursorPageVO.of(java.util.List.of(p), 20, MerchantPO::getId));

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
     * 验证 page 在无记录时返回空分页。
     */
    @Test
    public void page_returns_empty_when_no_records() {
        when(merchantService.page(any(com.yirancrazy.minimall.merchant.dto.MerchantPageDTO.class)))
                .thenReturn(CursorPageVO.empty(20));

        InternalPageQuery query = new InternalPageQuery();
        Result<CursorPageVO<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO>> r = controller.page(query);

        assertEquals(0, r.getData().getRecords().size());
    }

    /**
     * 验证 detail 正常返回。
     */
    @Test
    public void detail_returns_vo() {
        when(merchantService.detail(1L)).thenReturn(buildMerchant(1L, 100L, "shopA"));

        Result<com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO> r = controller.detail(1L);

        assertEquals("shopA", r.getData().getMerchantName());
    }

    /**
     * 验证 detail 在商家不存在时抛 MERCHANT_NOT_FOUND。
     */
    @Test
    public void detail_throws_when_not_found() {
        when(merchantService.detail(99L)).thenThrow(new BizException(
                com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum.MERCHANT_NOT_FOUND));

        assertThrows(BizException.class, () -> controller.detail(99L));
    }

    private MerchantPO buildMerchant(Long merchantId, Long userId, String name) {
        MerchantPO p = new MerchantPO();
        p.setId(merchantId);
        p.setUserId(userId);
        p.setMerchantName(name);
        p.setLicenseNo("L1");
        p.setAuditStatus(1);
        return p;
    }
}
