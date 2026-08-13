package com.yirancrazy.minimall.platform.controller.v1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.RolePermissionUpdateDTO;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.PermissionVO;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformRoleControllerV1 单元测试，验证角色权限查询与修改接口的薄控制器行为。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 **/
@ExtendWith(MockitoExtension.class)
class PlatformRoleControllerV1Test {

    @Mock private RolePermissionService rolePermissionService;

    private PlatformRoleControllerV1 controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformRoleControllerV1(rolePermissionService);
    }

    @Test
    void listRoles_returns_service_result() {
        RoleVO role = new RoleVO("PLATFORM", "平台管理员",
            List.of(new PermissionVO("ORDER_VIEW_ALL", "查看全部订单")));
        when(rolePermissionService.listRoles()).thenReturn(List.of(role));

        Result<List<RoleVO>> result = controller.listRoles();

        assertEquals("00000", result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("PLATFORM", result.getData().get(0).getRoleCode());
    }

    @Test
    void getRolePermissions_valid_code_returns_role() {
        RoleVO role = new RoleVO("USER", "用户",
            List.of(new PermissionVO("CART_MANAGE", "购物车管理")));
        when(rolePermissionService.getRolePermissions("USER")).thenReturn(role);

        Result<RoleVO> result = controller.getRolePermissions("USER");

        assertEquals("00000", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getPermissions().size() > 0);
    }

    @Test
    void getRolePermissions_invalid_code_returns_null() {
        when(rolePermissionService.getRolePermissions("UNKNOWN")).thenReturn(null);

        Result<RoleVO> result = controller.getRolePermissions("UNKNOWN");

        assertEquals("00000", result.getCode());
        assertNull(result.getData());
    }

    @Test
    void updatePermissions_success_delegates_to_service() {
        RolePermissionUpdateDTO dto = new RolePermissionUpdateDTO();
        dto.setPermissionCodes(List.of("GOODS_VIEW", "GOODS_MANAGE"));

        Result<Void> result = controller.updatePermissions("MERCHANT", dto);

        assertEquals("00000", result.getCode());
        verify(rolePermissionService).updatePermissions("MERCHANT", List.of("GOODS_VIEW", "GOODS_MANAGE"));
    }

    @Test
    void updatePermissions_service_exception_is_propagated() {
        RolePermissionUpdateDTO dto = new RolePermissionUpdateDTO();
        dto.setPermissionCodes(List.of("GOODS_VIEW"));
        org.mockito.Mockito.doThrow(new BizException("12011", "ROLE_PERMISSION_NOT_MODIFIABLE", "该角色不允许修改权限"))
            .when(rolePermissionService).updatePermissions("USER", List.of("GOODS_VIEW"));

        BizException ex = assertThrows(BizException.class,
            () -> controller.updatePermissions("USER", dto));

        assertEquals("12011", ex.getCode());
    }
}
