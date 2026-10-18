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
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.service.UserService;
import com.yirancrazy.minimall.user.vo.UserVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class UserControllerV1Test {

    private MockMvc mockMvc;
    private UserService userService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserControllerV1(userService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }


    @Test
    void page_returns_cursor_result() throws Exception {
        when(userService.pageVO(any(com.yirancrazy.minimall.user.dto.UserPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/user/users").header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(userService).pageVO(any(com.yirancrazy.minimall.user.dto.UserPageDTO.class));
    }


    @Test
    void get_returns_user() throws Exception {
        UserVO vo = new UserVO();
        vo.setId(99L);
        vo.setUsername("yiran");
        vo.setNickname("薄荷");
        when(userService.getVO(99L)).thenReturn(vo);
        mockMvc.perform(get("/api/v1/user/users/99").header("X-User-Role", "PLATFORM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.username").value("yiran"));
        verify(userService).getVO(99L);
    }


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


    @Test
    void page_rejects_non_platform_role() throws Exception {
        mockMvc.perform(get("/api/v1/user/users").header("X-User-Role", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).pageVO(any(com.yirancrazy.minimall.user.dto.UserPageDTO.class));
    }


    @Test
    void get_rejects_non_platform_role() throws Exception {
        mockMvc.perform(get("/api/v1/user/users/99").header("X-User-Role", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).getVO(99L);
    }


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


    @Test
    void delete_rejects_non_platform_role() throws Exception {
        mockMvc.perform(delete("/api/v1/user/users/99").header("X-User-Role", "USER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("20003"));
        verify(userService, never()).delete(99L);
    }


    @Test
    void get_profile_with_header_returns_vo() throws Exception {
        UserVO vo = new UserVO();
        vo.setId(1L);
        vo.setUsername("yiran");
        vo.setNickname("薄荷");
        when(userService.getProfileVO(anyLong())).thenReturn(vo);
        mockMvc.perform(get("/api/v1/user/users/profile").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(1));
        verify(userService).getProfileVO(anyLong());
    }


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
