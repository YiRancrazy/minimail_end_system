package com.yirancrazy.minimall.user.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.user.dto.UserProfileDto;
import com.yirancrazy.minimall.user.dto.UserUpdateDto;
import com.yirancrazy.minimall.user.service.UserService;
import com.yirancrazy.minimall.user.vo.UserVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户控制器单元测试，使用 MockMvc 覆盖平台权限校验、用户管理响应和基于请求头的资料查询与更新.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class UserControllerV1Test {

    private MockMvc mockMvc;
    private UserService userService;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 初始化用户服务替身、控制器和 MockMvc 异常处理器。
     */
    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new UserControllerV1(userService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    /** 验证平台分页：角色 PLATFORM、服务返回空页，调用 GET /users，预期 00000 并调用 pageVo. */
    @Test
    void testPageReturnsCursorResult() throws Exception {
        when(userService.pageVo(any(com.yirancrazy.minimall.user.dto.UserPageDto.class)))
                .thenReturn(new CursorPageVo<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc
                .perform(get("/api/v1/user/users").header("X-User-Role", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(userService).pageVo(any(com.yirancrazy.minimall.user.dto.UserPageDto.class));
    }

    /** 验证平台详情：角色 PLATFORM、用户 99 昵称“薄荷”，预期 data.id=99 且昵称一致. */
    @Test
    void testGetReturnsUser() throws Exception {
        UserVo vo = new UserVo();
        vo.setId(99L);
        vo.setNickname("薄荷");
        when(userService.getVo(99L)).thenReturn(vo);
        mockMvc
                .perform(get("/api/v1/user/users/99").header("X-User-Role", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.nickname").value("薄荷"));
        verify(userService).getVo(99L);
    }

    /** 验证创建端点移除：角色 PLATFORM，POST /users，预期全局异常处理返回 20000. */
    @Test
    void testCreateEndpointNoLongerExists() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/user/users")
                                .header("X-User-Role", "PLATFORM")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nickname\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20000"));
    }

    /** 验证分页越权：角色 USER，调用 GET /users，预期 20003 且不调用 pageVo. */
    @Test
    void testPageRejectsNonPlatformRole() throws Exception {
        mockMvc
                .perform(get("/api/v1/user/users").header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).pageVo(any(com.yirancrazy.minimall.user.dto.UserPageDto.class));
    }

    /** 验证详情越权：角色 USER，调用 GET /users/99，预期 20003 且不调用 getVo. */
    @Test
    void testGetRejectsNonPlatformRole() throws Exception {
        mockMvc
                .perform(get("/api/v1/user/users/99").header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).getVo(99L);
    }

    /** 验证更新越权：角色 USER、请求体合法，调用 PUT /users/99，预期 20003 且不调用 update. */
    @Test
    void testUpdateRejectsNonPlatformRole() throws Exception {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setNickname("薄荷");
        mockMvc
                .perform(
                        put("/api/v1/user/users/99")
                                .header("X-User-Role", "USER")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).update(eq(99L), any(UserUpdateDto.class));
    }

    /** 验证删除越权：角色 USER，调用 DELETE /users/99，预期 20003 且不调用 delete. */
    @Test
    void testDeleteRejectsNonPlatformRole() throws Exception {
        mockMvc
                .perform(delete("/api/v1/user/users/99").header("X-User-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).delete(99L);
    }

    /** 验证资料查询：X-User-Id=1、服务返回用户 1，调用 GET /users/profile，预期 00000、data.id=1. */
    @Test
    void testGetProfileWithHeaderReturnsVo() throws Exception {
        UserVo vo = new UserVo();
        vo.setId(1L);
        vo.setNickname("薄荷");
        when(userService.getProfileVo(anyLong())).thenReturn(vo);
        mockMvc
                .perform(get("/api/v1/user/users/profile").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(1));
        verify(userService).getProfileVo(anyLong());
    }

    /** 验证资料更新：X-User-Id=1、服务返回 true，PATCH /users/profile，预期 00000、data=true. */
    @Test
    void testUpdateProfileWithHeaderInvokesService() throws Exception {
        when(userService.updateProfile(anyLong(), any(UserProfileDto.class))).thenReturn(true);
        UserProfileDto dto = new UserProfileDto();
        dto.setNickname("新昵称");
        dto.setAvatar("https://example.com/a.png");
        dto.setGender(1);
        mockMvc
                .perform(
                        patch("/api/v1/user/users/profile")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(userService).updateProfile(anyLong(), any(UserProfileDto.class));
    }
}
