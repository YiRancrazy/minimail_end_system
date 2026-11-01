package com.yirancrazy.minimall.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.vo.RoleVo;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色权限查询、校验和 Redis 写入行为的单元测试.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolePermissionServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RolePermissionServiceImpl service;

    /** 配置 Redis 替身返回空覆盖值，并为每个测试创建角色权限服务。 */
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        service = new RolePermissionServiceImpl(redisTemplate);
    }

    /** 目标：验证未知角色查询；前置：枚举无 UNKNOWN；步骤：调用 getRolePermissions("UNKNOWN")；预期：返回 null. */
    @Test
    void testGetRolePermissionsUnknownRoleReturnsNull() {
        assertNull(service.getRolePermissions("UNKNOWN"));
    }

    /** 目标：验证未知角色更新；前置：枚举无 UNKNOWN；步骤：用有效权限码更新；预期：BizException code=12010. */
    @Test
    void testUpdatePermissionsUnknownRoleThrowsRoleNotFound() {
        BizException ex =
                assertThrows(
                        BizException.class, () -> service.updatePermissions("UNKNOWN", List.of("GOODS_VIEW")));
        assertEquals(PlatformCodeEnum.ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    /** 目标：验证 USER 角色禁改；前置：映射规则禁止修改 USER；步骤：用有效权限码更新；预期：code=12011. */
    @Test
    void testUpdatePermissionsUserRoleThrowsNotModifiable() {
        BizException ex =
                assertThrows(
                        BizException.class, () -> service.updatePermissions("USER", List.of("GOODS_VIEW")));
        assertEquals(PlatformCodeEnum.ROLE_PERMISSION_NOT_MODIFIABLE.getCode(), ex.getCode());
    }

    /** 目标：验证无效权限码映射；前置：权限码无效；步骤：更新 MERCHANT；预期：code=12012. */
    @Test
    void testUpdatePermissionsInvalidPermissionCodeThrowsUpdateFailed() {
        BizException ex =
                assertThrows(
                        BizException.class,
                        () -> service.updatePermissions("MERCHANT", List.of("NOT_A_PERMISSION")));
        assertEquals(PlatformCodeEnum.ROLE_PERMISSION_UPDATE_FAILED.getCode(), ex.getCode());
    }

    /** 目标：验证权限写 Redis；前置：MERCHANT 可修改且记录 value 操作；步骤：更新两个权限码；预期：写入覆盖键及 JSON 数组. */
    @Test
    void testUpdatePermissionsValidRoleWritesRedis() {
        service.updatePermissions("MERCHANT", List.of("GOODS_VIEW", "GOODS_MANAGE"));
        verify(valueOperations)
                .set("platform:role-permissions:MERCHANT", "[\"GOODS_VIEW\",\"GOODS_MANAGE\"]");
    }

    /** 目标：验证全部角色权限列表；前置：Redis 无覆盖值；步骤：调用 listRoles；预期：返回 3 个角色且编码、权限均非空. */
    @Test
    void testListRolesReturnsAllRolesWithPermissions() {
        List<RoleVo> roles = service.listRoles();
        assertEquals(3, roles.size());
        for (RoleVo vo : roles) {
            assertNotNull(vo.getRoleCode());
            assertFalse(vo.getPermissions().isEmpty());
        }
    }
}
