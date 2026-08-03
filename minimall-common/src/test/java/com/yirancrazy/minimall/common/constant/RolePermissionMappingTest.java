package com.yirancrazy.minimall.common.constant;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RolePermissionMapping 单元测试，验证角色-权限映射的完整性与正确性。
 */
class RolePermissionMappingTest {

    @Test
    void user_has_expected_permissions() {
        Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.USER);
        assertEquals(8, perms.size());
        assertTrue(perms.contains(PermissionEnum.GOODS_VIEW));
        assertTrue(perms.contains(PermissionEnum.CART_MANAGE));
        assertTrue(perms.contains(PermissionEnum.ORDER_CREATE));
        assertTrue(perms.contains(PermissionEnum.ORDER_VIEW));
        assertTrue(perms.contains(PermissionEnum.ORDER_CANCEL));
        assertTrue(perms.contains(PermissionEnum.PAY_VIEW));
        assertTrue(perms.contains(PermissionEnum.NOTIFY_VIEW));
        assertTrue(perms.contains(PermissionEnum.PROFILE_MANAGE));
    }

    @Test
    void merchant_has_expected_permissions() {
        Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.MERCHANT);
        assertEquals(8, perms.size());
        assertTrue(perms.contains(PermissionEnum.GOODS_MANAGE));
        assertTrue(perms.contains(PermissionEnum.STOCK_MANAGE));
        assertTrue(perms.contains(PermissionEnum.ORDER_SHIP));
        assertTrue(perms.contains(PermissionEnum.QUALIFICATION_SUBMIT));
    }

    @Test
    void platform_has_expected_permissions() {
        Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.PLATFORM);
        assertTrue(perms.contains(PermissionEnum.MERCHANT_AUDIT));
        assertTrue(perms.contains(PermissionEnum.GOODS_AUDIT));
        assertTrue(perms.contains(PermissionEnum.ORDER_VIEW_ALL));
        assertTrue(perms.contains(PermissionEnum.ORDER_DELETE));
        assertTrue(perms.contains(PermissionEnum.REFUND_AUDIT));
        assertTrue(perms.contains(PermissionEnum.ROLE_VIEW));
        assertTrue(perms.contains(PermissionEnum.NOTIFY_BROADCAST));
        assertTrue(perms.contains(PermissionEnum.PLATFORM_FINANCE));
    }

    @Test
    void user_cannot_access_platform_permissions() {
        assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, PermissionEnum.MERCHANT_AUDIT));
        assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, PermissionEnum.ORDER_DELETE));
        assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, PermissionEnum.ROLE_VIEW));
    }

    @Test
    void merchant_cannot_access_platform_permissions() {
        assertFalse(RolePermissionMapping.hasPermission(RoleEnum.MERCHANT, PermissionEnum.GOODS_AUDIT));
        assertFalse(RolePermissionMapping.hasPermission(RoleEnum.MERCHANT, PermissionEnum.PLATFORM_FINANCE));
    }

    @Test
    void null_role_returns_empty_permissions() {
        Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(null);
        assertTrue(perms.isEmpty());
    }

    @Test
    void hasPermission_with_null_args_returns_false() {
        assertFalse(RolePermissionMapping.hasPermission(null, PermissionEnum.GOODS_VIEW));
        assertFalse(RolePermissionMapping.hasPermission(RoleEnum.USER, null));
    }

    @Test
    void permissions_set_is_immutable() {
        Set<PermissionEnum> perms = RolePermissionMapping.permissionsOf(RoleEnum.USER);
        try {
            perms.add(PermissionEnum.ORDER_DELETE);
            assertFalse(true, "should have thrown UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e) {
            assertTrue(true);
        }
    }
}
