package com.yirancrazy.minimall.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDto;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家服务实现的远程委派、空结果和异常映射测试场景.
 * 当前四个场景方法均未标注 @Test，JUnit 不会自动执行这些方法。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class PlatformMerchantServiceImplTest {

    private PlatformRemoteGateway platformRemoteGateway;
    private MinioUtil minioUtil;
    private PlatformMerchantServiceImpl service;

    /** 使用远程网关与 MinIO 工具替身为每个场景创建商家服务实例。 */
    @BeforeEach
    void setUp() {
        platformRemoteGateway = mock(PlatformRemoteGateway.class);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformMerchantServiceImpl(platformRemoteGateway, minioUtil);
    }

    /**
     * 验证商家分页结果被映射为平台商家视图。
     *
     * <p>前置条件：远程网关返回一个 shopA 商家记录。
     * <p>关键步骤：以关键字 shop、审核状态 1、limit=20 查询分页。
     * <p>预期结果：分页仅含一条记录且商家名为 shopA，并委派远程分页接口。
     */
    public void pageDelegatesToFeign() {
        MerchantManageVo vo = new MerchantManageVo(1L, 100L, "shopA", "L1", null, 1, null, null, null);
        when(platformRemoteGateway.pageMerchants(any(InternalPageQuery.class)))
                .thenReturn(
                        Result.success(CursorPageVo.of(List.of(vo), 20, MerchantManageVo::getMerchantId)));

        PlatformMerchantPageDto dto = new PlatformMerchantPageDto();
        dto.setLimit(20);
        dto.setKeyword("shop");
        dto.setAuditStatus(1);
        CursorPageVo<com.yirancrazy.minimall.platform.vo.PlatformMerchantVo> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        assertEquals("shopA", page.getRecords().get(0).getMerchantName());
        verify(platformRemoteGateway).pageMerchants(any(InternalPageQuery.class));
    }

    /**
     * 验证远程分页数据为空时返回保持请求 limit 的空分页。
     *
     * <p>前置条件：远程网关返回成功响应但 data 为 null。
     * <p>关键步骤：以 limit=20 查询商家分页。
     * <p>预期结果：记录数为 0，分页 limit 为 20。
     */
    public void pageReturnsEmptyWhenFeignDataNull() {
        when(platformRemoteGateway.pageMerchants(any(InternalPageQuery.class)))
                .thenReturn(Result.success(null));

        PlatformMerchantPageDto dto = new PlatformMerchantPageDto();
        dto.setLimit(20);
        CursorPageVo<com.yirancrazy.minimall.platform.vo.PlatformMerchantVo> page = service.page(dto);

        assertEquals(0, page.getRecords().size());
        assertEquals(20, page.getLimit());
    }

    /**
     * 验证商家详情被转换为平台商家视图。
     *
     * <p>前置条件：远程网关对商家 1 返回 shopA 数据。
     * <p>关键步骤：调用 detail(1)。
     * <p>预期结果：返回视图中的商家名为 shopA。
     */
    public void detailReturnsVo() {
        when(platformRemoteGateway.merchantDetail(1L))
                .thenReturn(
                        Result.success(
                                new MerchantManageVo(1L, 100L, "shopA", "L1", null, 1, null, null, null)));

        com.yirancrazy.minimall.platform.vo.PlatformMerchantVo vo = service.detail(1L);
        assertEquals("shopA", vo.getMerchantName());
    }

    /**
     * 验证商家详情不存在时抛出业务异常。
     *
     * <p>前置条件：远程网关对商家 99 返回成功响应但 data 为 null。
     * <p>关键步骤：调用 detail(99)。
     * <p>预期结果：抛出 BizException。
     */
    public void detailThrowsWhenFeignDataNull() {
        when(platformRemoteGateway.merchantDetail(99L)).thenReturn(Result.success(null));

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
