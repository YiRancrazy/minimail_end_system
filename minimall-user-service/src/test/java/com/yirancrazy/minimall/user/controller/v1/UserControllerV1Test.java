package com.yirancrazy.minimall.user.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.service.UserService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserControllerV1 MockMvc 单元测试，验证用户分页查询、CRUD 与个人资料管理等端点的 HTTP 路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class UserControllerV1Test {

    private MockMvc mockMvc;
    private UserService userService;
    private MinioUtil minioUtil;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        minioUtil = mock(MinioUtil.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserControllerV1(userService, minioUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    /**
     * 验证 PLATFORM 角色 GET / 返回游标分页结果。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(userService.page(any(com.yirancrazy.minimall.user.dto.UserPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/user/users").header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(userService).page(any(com.yirancrazy.minimall.user.dto.UserPageDTO.class));
    }

    /**
     * 验证 PLATFORM 角色 GET /{id} 返回用户详情。
     */
    @Test
    void get_returns_user() throws Exception {
        UserPO po = new UserPO();
        po.setId(99L);
        po.setUsername("yiran");
        po.setNickname("薄荷");
        when(userService.getById(99L)).thenReturn(po);
        mockMvc.perform(get("/api/v1/user/users/99").header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.username").value("yiran"));
        verify(userService).getById(99L);
    }

    /**
     * 验证 PLATFORM 角色 POST / 创建用户并返回用户 ID。
     */
    @Test
    void create_returns_id() throws Exception {
        when(userService.create(any(UserCreateDTO.class))).thenReturn(100L);
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("yiran");
        dto.setNickname("薄荷");
        mockMvc.perform(post("/api/v1/user/users")
                .header("X-User-Role", "PLATFORM")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
        verify(userService).create(any(UserCreateDTO.class));
    }

    /**
     * 验证非 PLATFORM 角色访问分页端点时返回 FORBIDDEN 且不进入 service。
     */
    @Test
    void page_rejects_non_platform_role() throws Exception {
        mockMvc.perform(get("/api/v1/user/users").header("X-User-Role", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).page(any(com.yirancrazy.minimall.user.dto.UserPageDTO.class));
    }

    /**
     * 验证非 PLATFORM 角色访问用户详情端点时返回 FORBIDDEN 且不进入 service。
     */
    @Test
    void get_rejects_non_platform_role() throws Exception {
        mockMvc.perform(get("/api/v1/user/users/99").header("X-User-Role", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).getById(99L);
    }

    /**
     * 验证非 PLATFORM 角色访问更新端点时返回 FORBIDDEN 且不进入 service。
     */
    @Test
    void update_rejects_non_platform_role() throws Exception {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("yiran");
        dto.setNickname("薄荷");
        mockMvc.perform(put("/api/v1/user/users/99")
                .header("X-User-Role", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).update(eq(99L), any(UserUpdateDTO.class));
    }

    /**
     * 验证非 PLATFORM 角色访问删除端点时返回 FORBIDDEN 且不进入 service。
     */
    @Test
    void delete_rejects_non_platform_role() throws Exception {
        mockMvc.perform(delete("/api/v1/user/users/99").header("X-User-Role", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).delete(99L);
    }

    /**
     * 验证 GET /profile 带 X-User-Id 头返回当前用户个人资料。
     */
    @Test
    void get_profile_with_header_returns_vo() throws Exception {
        UserPO po = new UserPO();
        po.setId(1L);
        po.setUsername("yiran");
        po.setNickname("薄荷");
        when(userService.getProfile(anyLong())).thenReturn(po);
        mockMvc.perform(get("/api/v1/user/users/profile").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(1));
        verify(userService).getProfile(anyLong());
    }

    /**
     * 验证 PATCH /profile 带 X-User-Id 头与请求体更新个人资料并返回成功。
     */
    @Test
    void update_profile_with_header_invokes_service() throws Exception {
        when(userService.updateProfile(anyLong(), any(UserProfileDTO.class))).thenReturn(true);
        UserProfileDTO dto = new UserProfileDTO();
        dto.setNickname("新昵称");
        dto.setAvatar("https://example.com/a.png");
        dto.setGender(1);
        mockMvc.perform(patch("/api/v1/user/users/profile")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(userService).updateProfile(anyLong(), any(UserProfileDTO.class));
    }
}
