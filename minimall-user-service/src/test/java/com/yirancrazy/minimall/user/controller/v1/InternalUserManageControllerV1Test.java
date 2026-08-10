package com.yirancrazy.minimall.user.controller.v1;

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
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;

/**
 * InternalUserManageControllerV1 单元测试，覆盖分页查询与详情查询的正常/失败路径。
 */
public class InternalUserManageControllerV1Test {

    private UserManager userManager;
    private InternalUserManageControllerV1 controller;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.user.mapper.UserMapper");
        TableInfoHelper.initTableInfo(assistant, UserPO.class);

        userManager = mock(UserManager.class);
        controller = new InternalUserManageControllerV1(userManager);
    }

    /**
     * 验证 page 在无 keyword 时返回空分页。
     */
    @Test
    public void page_returns_empty_when_no_records() {
        when(userManager.page(any(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class),
                any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        InternalPageQuery query = new InternalPageQuery();
        query.setLimit(20);
        Result<CursorPageVO<UserManageVO>> r = controller.page(query);

        assertEquals("00000", r.getCode());
        assertEquals(0, r.getData().getRecords().size());
    }

    /**
     * 验证 page 委托 manager 并按 keyword 模糊匹配。
     */
    @Test
    public void page_delegates_with_keyword() {
        UserPO u = buildUser(1L, "alice", "小荷");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserPO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setRecords(java.util.List.of(u));
        lenient().when(userManager.page(any(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class),
                any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(page);

        InternalPageQuery query = new InternalPageQuery();
        query.setLimit(20);
        query.setKeyword("alice");
        Result<CursorPageVO<UserManageVO>> r = controller.page(query);

        assertEquals(1, r.getData().getRecords().size());
        assertEquals("alice", r.getData().getRecords().get(0).getUsername());
    }

    /**
     * 验证 detail 正常返回。
     */
    @Test
    public void detail_returns_vo() {
        when(userManager.getById(1L)).thenReturn(buildUser(1L, "alice", "小荷"));

        Result<UserManageVO> r = controller.detail(1L);

        assertEquals("alice", r.getData().getUsername());
        assertEquals("138****0001", r.getData().getPhoneMasked());
    }

    /**
     * 验证 detail 在用户不存在时抛 USER_NOT_FOUND。
     */
    @Test
    public void detail_throws_when_not_found() {
        when(userManager.getById(99L)).thenReturn(null);

        assertThrows(BizException.class, () -> controller.detail(99L));
    }

    private UserPO buildUser(Long id, String username, String nickname) {
        UserPO u = new UserPO();
        u.setId(id);
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPhoneMasked("138****0001");
        u.setGender(1);
        return u;
    }
}
