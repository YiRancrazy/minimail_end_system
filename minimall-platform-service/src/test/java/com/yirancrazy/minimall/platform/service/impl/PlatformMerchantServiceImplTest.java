package com.yirancrazy.minimall.platform.service.impl;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;

/**
 * PlatformMerchantServiceImpl 单元测试，覆盖委托 MerchantFeignClient 的正常/失败/边界路径。
 */
public class PlatformMerchantServiceImplTest {

    private MerchantFeignClient merchantFeignClient;
    private PlatformMerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        merchantFeignClient = mock(MerchantFeignClient.class);
        service = new PlatformMerchantServiceImpl(merchantFeignClient);
    }

    /**
     * 验证 page 委托 FeignClient 并按 dto 字段填充 InternalPageQuery。
     */
    @Test
    public void page_delegates_to_feign() {
        MerchantManageVO vo = new MerchantManageVO(1L, 100L, "shopA",
                "L1", null, 1, null, null, null);
        when(merchantFeignClient.pageManage(any(InternalPageQuery.class)))
                .thenReturn(Result.success(CursorPageVO.of(List.of(vo), 20, MerchantManageVO::getMerchantId)));

        PlatformMerchantPageDTO dto = new PlatformMerchantPageDTO();
        dto.setLimit(20);
        dto.setKeyword("shop");
        dto.setAuditStatus(1);
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformMerchantVO> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        assertEquals("shopA", page.getRecords().get(0).getMerchantName());
        verify(merchantFeignClient).pageManage(any(InternalPageQuery.class));
    }

    /**
     * 验证 page 在 Feign 返回 null data 时退化为空分页。
     */
    @Test
    public void page_returns_empty_when_feign_data_null() {
        when(merchantFeignClient.pageManage(any(InternalPageQuery.class)))
                .thenReturn(Result.success(null));

        PlatformMerchantPageDTO dto = new PlatformMerchantPageDTO();
        dto.setLimit(20);
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformMerchantVO> page = service.page(dto);

        assertEquals(0, page.getRecords().size());
        assertEquals(20, page.getLimit());
    }

    /**
     * 验证 detail 正常返回。
     */
    @Test
    public void detail_returns_vo() {
        when(merchantFeignClient.detail(1L))
                .thenReturn(Result.success(new MerchantManageVO(1L, 100L, "shopA",
                        "L1", null, 1, null, null, null)));

        com.yirancrazy.minimall.platform.vo.PlatformMerchantVO vo = service.detail(1L);
        assertEquals("shopA", vo.getMerchantName());
    }

    /**
     * 验证 detail 在 Feign 返回 null data 时抛 PLATFORM_MERCHANT_NOT_FOUND。
     */
    @Test
    public void detail_throws_when_feign_data_null() {
        when(merchantFeignClient.detail(99L)).thenReturn(Result.success(null));

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
