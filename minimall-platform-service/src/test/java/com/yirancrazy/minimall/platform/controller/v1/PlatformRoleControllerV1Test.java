package com.yirancrazy.minimall.platform.controller.v1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * PlatformRoleControllerV1 单元测试，验证角色权限查询接口返回的数据完整性。
 */
class PlatformRoleControllerV1Test {

    private final PlatformRoleControllerV1 controller = new PlatformRoleControllerV1();

    @Test
    void listRoles_returns_all_three_roles() {
        Result<java.util.List<RoleVO>> result = controller.listRoles();
        assertEquals("00000", result.getCode());
        assertEquals(3, result.getData().size());
    }

    @Test
    void listRoles_each_role_has_permissions() {
        Result<java.util.List<RoleVO>> result = controller.listRoles();
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
}
