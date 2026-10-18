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
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class PlatformMerchantServiceImplTest {

    private PlatformRemoteGateway platformRemoteGateway;
    private MinioUtil minioUtil;
    private PlatformMerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        platformRemoteGateway = mock(PlatformRemoteGateway.class);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformMerchantServiceImpl(platformRemoteGateway, minioUtil);
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_to_feign() {
        MerchantManageVO vo = new MerchantManageVO(1L, 100L, "shopA",
                "L1", null, 1, null, null, null);
        when(platformRemoteGateway.pageMerchants(any(InternalPageQuery.class)))
                .thenReturn(Result.success(CursorPageVO.of(List.of(vo), 20, MerchantManageVO::getMerchantId)));

        PlatformMerchantPageDTO dto = new PlatformMerchantPageDTO();
        dto.setLimit(20);
        dto.setKeyword("shop");
        dto.setAuditStatus(1);
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformMerchantVO> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        assertEquals("shopA", page.getRecords().get(0).getMerchantName());
        verify(platformRemoteGateway).pageMerchants(any(InternalPageQuery.class));
    }


    /**
     * 分页查询数据。
     */
    public void page_returns_empty_when_feign_data_null() {
        when(platformRemoteGateway.pageMerchants(any(InternalPageQuery.class)))
                .thenReturn(Result.success(null));

        PlatformMerchantPageDTO dto = new PlatformMerchantPageDTO();
        dto.setLimit(20);
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformMerchantVO> page = service.page(dto);

        assertEquals(0, page.getRecords().size());
        assertEquals(20, page.getLimit());
    }


    /**
     * 执行 detail_returns_vo 方法。
     */
    public void detail_returns_vo() {
        when(platformRemoteGateway.merchantDetail(1L))
                .thenReturn(Result.success(new MerchantManageVO(1L, 100L, "shopA",
                        "L1", null, 1, null, null, null)));

        com.yirancrazy.minimall.platform.vo.PlatformMerchantVO vo = service.detail(1L);
        assertEquals("shopA", vo.getMerchantName());
    }


    /**
     * 执行 detail_throws_when_feign_data_null 方法。
     */
    public void detail_throws_when_feign_data_null() {
        when(platformRemoteGateway.merchantDetail(99L)).thenReturn(Result.success(null));

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
