package com.yirancrazy.minimall.platform.controller.v1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.RolePermissionUpdateDTO;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformRoleControllerV1 单元测试，验证角色权限查询与修改接口返回的数据完整性。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformRoleControllerV1Test {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private PlatformRoleControllerV1 controller;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        controller = new PlatformRoleControllerV1(redisTemplate);
    }

    @Test
    void listRoles_returns_all_three_roles() {
        Result<List<RoleVO>> result = controller.listRoles();
        assertEquals("00000", result.getCode());
        assertEquals(3, result.getData().size());
    }

    @Test
    void listRoles_each_role_has_permissions() {
        Result<List<RoleVO>> result = controller.listRoles();
        for (RoleVO vo : result.getData()) {
            assertNotNull(vo.getRoleCode());
            assertNotNull(vo.getDescription());
            assertTrue(vo.getPermissions().size() > 0,
                "role " + vo.getRoleCode() + " should have permissions");
        }
    }

    @Test
    void getRolePermissions_valid_code_returns_role() {
        Result<RoleVO> result = controller.getRolePermissions("PLATFORM");
        assertEquals("00000", result.getCode());
        assertEquals("PLATFORM", result.getData().getRoleCode());
        assertTrue(result.getData().getPermissions().size() > 0);
    }

    @Test
    void getRolePermissions_invalid_code_returns_null() {
        Result<RoleVO> result = controller.getRolePermissions("UNKNOWN");
        assertEquals("00000", result.getCode());
        assertNull(result.getData());
    }

    @Test
    void getRolePermissions_user_role_has_cart_permission() {
        Result<RoleVO> result = controller.getRolePermissions("USER");
        assertEquals("USER", result.getData().getRoleCode());
        boolean hasCartManage = result.getData().getPermissions().stream()
            .anyMatch(p -> "CART_MANAGE".equals(p.getPermissionCode()));
        assertTrue(hasCartManage);
    }

    @Test
    void updatePermissions_success() {
        RolePermissionUpdateDTO dto = new RolePermissionUpdateDTO();
        dto.setPermissionCodes(List.of("GOODS_VIEW", "GOODS_MANAGE"));

        Result<Void> result = controller.updatePermissions("MERCHANT", dto);

        assertEquals("00000", result.getCode());
        verify(valueOperations).set(anyString(), anyString());
    }

    @Test
    void updatePermissions_userRole_forbidden() {
        RolePermissionUpdateDTO dto = new RolePermissionUpdateDTO();
        dto.setPermissionCodes(List.of("GOODS_VIEW"));

        Result<Void> result = controller.updatePermissions("USER", dto);

        assertEquals("12010", result.getCode());
    }
}
