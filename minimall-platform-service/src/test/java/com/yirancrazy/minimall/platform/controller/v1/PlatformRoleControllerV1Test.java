package com.yirancrazy.minimall.platform.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.RolePermissionUpdateDto;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.PermissionVo;
import com.yirancrazy.minimall.platform.vo.RoleVo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限控制器的查询、更新及异常传播单元测试.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class PlatformRoleControllerV1Test {

    @Mock private RolePermissionService rolePermissionService;

    private PlatformRoleControllerV1 controller;

    /** 使用 Mockito 角色权限服务替身为每个测试创建控制器实例。 */
    @BeforeEach
    void setUp() {
        controller = new PlatformRoleControllerV1(rolePermissionService);
    }

    /** 目标：验证角色列表返回服务结果；前置：服务返回一个 PLATFORM 角色；步骤：调用 listRoles；预期：成功且数据仅含该角色. */
    @Test
    void testListRolesReturnsServiceResult() {
        RoleVo role =
                new RoleVo("PLATFORM", "平台管理员", List.of(new PermissionVo("ORDER_VIEW_ALL", "查看全部订单")));
        when(rolePermissionService.listRoles()).thenReturn(List.of(role));

        Result<List<RoleVo>> result = controller.listRoles();

        assertEquals("00000", result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("PLATFORM", result.getData().get(0).getRoleCode());
    }

    /** 目标：验证已知角色权限查询；前置：USER 返回购物车管理权限；步骤：查询 USER；预期：成功、数据非空且权限列表非空. */
    @Test
    void testGetRolePermissionsValidCodeReturnsRole() {
        RoleVo role = new RoleVo("USER", "用户", List.of(new PermissionVo("CART_MANAGE", "购物车管理")));
        when(rolePermissionService.getRolePermissions("USER")).thenReturn(role);

        Result<RoleVo> result = controller.getRolePermissions("USER");

        assertEquals("00000", result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().getPermissions().size() > 0);
    }

    /** 目标：验证未知角色查询契约；前置：UNKNOWN 返回 null；步骤：查询 UNKNOWN；预期：统一响应成功但数据为 null. */
    @Test
    void testGetRolePermissionsInvalidCodeReturnsNull() {
        when(rolePermissionService.getRolePermissions("UNKNOWN")).thenReturn(null);

        Result<RoleVo> result = controller.getRolePermissions("UNKNOWN");

        assertEquals("00000", result.getCode());
        assertNull(result.getData());
    }

    /** 目标：验证权限更新委派；前置：请求含商品查看和管理权限；步骤：更新 MERCHANT；预期：成功且服务收到相同参数. */
    @Test
    void testUpdatePermissionsSuccessDelegatesToService() {
        RolePermissionUpdateDto dto = new RolePermissionUpdateDto();
        dto.setPermissionCodes(List.of("GOODS_VIEW", "GOODS_MANAGE"));

        Result<Void> result = controller.updatePermissions("MERCHANT", dto);

        assertEquals("00000", result.getCode());
        verify(rolePermissionService)
                .updatePermissions("MERCHANT", List.of("GOODS_VIEW", "GOODS_MANAGE"));
    }

    /** 目标：验证业务异常传播；前置：服务更新 USER 抛不可修改异常；步骤：调用更新接口；预期：BizException 传播且 code=12011. */
    @Test
    void testUpdatePermissionsServiceExceptionIsPropagated() {
        RolePermissionUpdateDto dto = new RolePermissionUpdateDto();
        dto.setPermissionCodes(List.of("GOODS_VIEW"));
        org.mockito.Mockito.doThrow(
                        new BizException("12011", "ROLE_PERMISSION_NOT_MODIFIABLE", "该角色不允许修改权限"))
                .when(rolePermissionService)
                .updatePermissions("USER", List.of("GOODS_VIEW"));

        BizException ex =
                assertThrows(BizException.class, () -> controller.updatePermissions("USER", dto));

        assertEquals("12011", ex.getCode());
    }
}
