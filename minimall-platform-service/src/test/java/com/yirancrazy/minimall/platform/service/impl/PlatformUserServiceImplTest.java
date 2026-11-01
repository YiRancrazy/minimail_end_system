package com.yirancrazy.minimall.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDto;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户服务实现的远程委派、空结果和异常映射单元测试.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class PlatformUserServiceImplTest {

    private PlatformRemoteGateway platformRemoteGateway;
    private MinioUtil minioUtil;
    private PlatformUserServiceImpl service;

    /** 使用远程网关与 MinIO 工具替身为每个测试创建用户服务实例。 */
    @BeforeEach
    void setUp() {
        platformRemoteGateway = mock(PlatformRemoteGateway.class);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformUserServiceImpl(platformRemoteGateway, minioUtil);
    }

    /** 目标：验证用户分页映射；前置：远程返回小荷记录；步骤：按 alice、limit=20 查询；预期：仅一条记录、昵称正确且委派远程接口. */
    @Test
    public void testPageDelegatesToFeign() {
        UserManageVo vo = new UserManageVo(1L, "小荷", null, "138****0001", 1, LocalDateTime.now());
        when(platformRemoteGateway.pageUsers(any(InternalPageQuery.class)))
                .thenReturn(Result.success(CursorPageVo.of(List.of(vo), 20, UserManageVo::getId)));

        PlatformUserPageDto dto = new PlatformUserPageDto();
        dto.setLimit(20);
        dto.setKeyword("alice");
        CursorPageVo<com.yirancrazy.minimall.platform.vo.PlatformUserVo> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        assertEquals("小荷", page.getRecords().get(0).getNickname());
        verify(platformRemoteGateway).pageUsers(any(InternalPageQuery.class));
    }

    /** 目标：验证空分页降级；前置：远程成功响应但 data=null；步骤：按 limit=20 查询；预期：返回记录数为 0 的分页. */
    @Test
    public void testPageReturnsEmptyWhenFeignDataNull() {
        when(platformRemoteGateway.pageUsers(any(InternalPageQuery.class)))
                .thenReturn(Result.success(null));

        PlatformUserPageDto dto = new PlatformUserPageDto();
        dto.setLimit(20);
        CursorPageVo<com.yirancrazy.minimall.platform.vo.PlatformUserVo> page = service.page(dto);

        assertEquals(0, page.getRecords().size());
    }

    /** 目标：验证用户详情映射；前置：用户 1 返回小荷资料；步骤：调用 detail(1)；预期：视图中的昵称为小荷. */
    @Test
    public void testDetailReturnsVo() {
        when(platformRemoteGateway.userDetail(1L))
                .thenReturn(
                        Result.success(
                                new UserManageVo(1L, "小荷", null, "138****0001", 1, LocalDateTime.now())));

        com.yirancrazy.minimall.platform.vo.PlatformUserVo vo = service.detail(1L);
        assertEquals("小荷", vo.getNickname());
    }

    /** 目标：验证详情缺失异常；前置：用户 99 返回 data=null；步骤：调用 detail(99)；预期：抛出 BizException. */
    @Test
    public void testDetailThrowsWhenFeignDataNull() {
        when(platformRemoteGateway.userDetail(99L)).thenReturn(Result.success(null));

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
