package com.yirancrazy.minimall.platform.service.impl;

import java.time.LocalDateTime;
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
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformUserServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class PlatformUserServiceImplTest {

    private PlatformRemoteGateway platformRemoteGateway;
    private MinioUtil minioUtil;
    private PlatformUserServiceImpl service;

    @BeforeEach
    void setUp() {
        platformRemoteGateway = mock(PlatformRemoteGateway.class);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformUserServiceImpl(platformRemoteGateway, minioUtil);
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_to_feign() {
        UserManageVO vo = new UserManageVO(1L, "alice", "小荷",
                null, "138****0001", 1, LocalDateTime.now());
        when(platformRemoteGateway.pageUsers(any(InternalPageQuery.class)))
                .thenReturn(Result.success(CursorPageVO.of(List.of(vo), 20, UserManageVO::getId)));

        PlatformUserPageDTO dto = new PlatformUserPageDTO();
        dto.setLimit(20);
        dto.setKeyword("alice");
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformUserVO> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        assertEquals("alice", page.getRecords().get(0).getUsername());
        verify(platformRemoteGateway).pageUsers(any(InternalPageQuery.class));
    }


    /**
     * 分页查询数据。
     */
    public void page_returns_empty_when_feign_data_null() {
        when(platformRemoteGateway.pageUsers(any(InternalPageQuery.class)))
                .thenReturn(Result.success(null));

        PlatformUserPageDTO dto = new PlatformUserPageDTO();
        dto.setLimit(20);
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformUserVO> page = service.page(dto);

        assertEquals(0, page.getRecords().size());
    }


    /**
     * 执行 detail_returns_vo 方法。
     */
    public void detail_returns_vo() {
        when(platformRemoteGateway.userDetail(1L))
                .thenReturn(Result.success(new UserManageVO(1L, "alice", "小荷",
                        null, "138****0001", 1, LocalDateTime.now())));

        com.yirancrazy.minimall.platform.vo.PlatformUserVO vo = service.detail(1L);
        assertEquals("alice", vo.getUsername());
    }


    /**
     * 执行 detail_throws_when_feign_data_null 方法。
     */
    public void detail_throws_when_feign_data_null() {
        when(platformRemoteGateway.userDetail(99L)).thenReturn(Result.success(null));

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
