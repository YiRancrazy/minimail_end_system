package com.yirancrazy.minimall.user.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalUserManageControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class InternalUserManageControllerV1Test {

    private UserService userService;
    private InternalUserManageControllerV1 controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new InternalUserManageControllerV1(userService);
    }


    /**
     * 分页查询数据。
     */
    public void page_returns_empty_when_no_records() {
        when(userService.pageManage(any(InternalPageQuery.class)))
                .thenReturn(CursorPageVO.of(java.util.List.of(), 20, UserManageVO::getId));

        InternalPageQuery query = new InternalPageQuery();
        query.setLimit(20);
        Result<CursorPageVO<UserManageVO>> r = controller.page(query);

        assertEquals("00000", r.getCode());
        assertEquals(0, r.getData().getRecords().size());
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_with_keyword() {
        UserManageVO user = buildUser(1L, "alice", "小荷");
        when(userService.pageManage(any(InternalPageQuery.class)))
                .thenReturn(CursorPageVO.of(java.util.List.of(user), 20, UserManageVO::getId));

        InternalPageQuery query = new InternalPageQuery();
        query.setLimit(20);
        query.setKeyword("alice");
        Result<CursorPageVO<UserManageVO>> r = controller.page(query);

        assertEquals(1, r.getData().getRecords().size());
        assertEquals("alice", r.getData().getRecords().get(0).getUsername());
    }


    /**
     * 执行 detail_returns_vo 方法。
     */
    public void detail_returns_vo() {
        when(userService.getManageDetail(1L)).thenReturn(buildUser(1L, "alice", "小荷"));

        Result<UserManageVO> r = controller.detail(1L);

        assertEquals("alice", r.getData().getUsername());
        assertEquals("138****0001", r.getData().getPhoneMasked());
    }


    /**
     * 执行 detail_throws_when_not_found 方法。
     */
    public void detail_throws_when_not_found() {
        when(userService.getManageDetail(99L))
                .thenThrow(new BizException(com.yirancrazy.minimall.user.constant.UserCodeEnum.USER_NOT_FOUND));

        assertThrows(BizException.class, () -> controller.detail(99L));
    }

    private UserManageVO buildUser(Long id, String username, String nickname) {
        UserManageVO u = new UserManageVO();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPhoneMasked("138****0001");
        u.setGender(1);
        return u;
    }
}
