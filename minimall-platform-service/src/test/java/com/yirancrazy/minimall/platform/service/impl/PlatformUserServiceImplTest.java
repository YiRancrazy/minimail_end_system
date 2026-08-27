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
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;

/**
 * PlatformUserServiceImpl 单元测试，覆盖委托 UserFeignClient 的正常/失败/边界路径。
 */
public class PlatformUserServiceImplTest {

    private UserFeignClient userFeignClient;
    private MinioUtil minioUtil;
    private PlatformUserServiceImpl service;

    @BeforeEach
    void setUp() {
        userFeignClient = mock(UserFeignClient.class);
        minioUtil = mock(MinioUtil.class);
        service = new PlatformUserServiceImpl(userFeignClient, minioUtil);
    }

    /**
     * 验证 page 委托 FeignClient。
     */
    @Test
    public void page_delegates_to_feign() {
        UserManageVO vo = new UserManageVO(1L, "alice", "小荷",
                null, "138****0001", 1, LocalDateTime.now());
        when(userFeignClient.pageManage(any(InternalPageQuery.class)))
                .thenReturn(Result.success(CursorPageVO.of(List.of(vo), 20, UserManageVO::getId)));

        PlatformUserPageDTO dto = new PlatformUserPageDTO();
        dto.setLimit(20);
        dto.setKeyword("alice");
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformUserVO> page = service.page(dto);

        assertEquals(1, page.getRecords().size());
        assertEquals("alice", page.getRecords().get(0).getUsername());
        verify(userFeignClient).pageManage(any(InternalPageQuery.class));
    }

    /**
     * 验证 page 在 Feign 返回 null data 时退化为空分页。
     */
    @Test
    public void page_returns_empty_when_feign_data_null() {
        when(userFeignClient.pageManage(any(InternalPageQuery.class)))
                .thenReturn(Result.success(null));

        PlatformUserPageDTO dto = new PlatformUserPageDTO();
        dto.setLimit(20);
        CursorPageVO<com.yirancrazy.minimall.platform.vo.PlatformUserVO> page = service.page(dto);

        assertEquals(0, page.getRecords().size());
    }

    /**
     * 验证 detail 正常返回。
     */
    @Test
    public void detail_returns_vo() {
        when(userFeignClient.detail(1L))
                .thenReturn(Result.success(new UserManageVO(1L, "alice", "小荷",
                        null, "138****0001", 1, LocalDateTime.now())));

        com.yirancrazy.minimall.platform.vo.PlatformUserVO vo = service.detail(1L);
        assertEquals("alice", vo.getUsername());
    }

    /**
     * 验证 detail 在 Feign 返回 null data 时抛 PLATFORM_USER_NOT_FOUND。
     */
    @Test
    public void detail_throws_when_feign_data_null() {
        when(userFeignClient.detail(99L)).thenReturn(Result.success(null));

        assertThrows(BizException.class, () -> service.detail(99L));
    }
}
