package com.yirancrazy.minimall.platform.service.impl;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RolePermissionServiceImpl 单元测试，验证角色不存在、权限不可修改与写入等分支。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolePermissionServiceImplTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RolePermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        service = new RolePermissionServiceImpl(redisTemplate);
    }

    @Test
    void getRolePermissions_unknownRole_returnsNull() {
        assertNull(service.getRolePermissions("UNKNOWN"));
    }

    @Test
    void updatePermissions_unknownRole_throwsRoleNotFound() {
        BizException ex = assertThrows(BizException.class,
            () -> service.updatePermissions("UNKNOWN", List.of("GOODS_VIEW")));
        assertEquals(PlatformCodeEnum.ROLE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void updatePermissions_userRole_throwsNotModifiable() {
        BizException ex = assertThrows(BizException.class,
            () -> service.updatePermissions("USER", List.of("GOODS_VIEW")));
        assertEquals(PlatformCodeEnum.ROLE_PERMISSION_NOT_MODIFIABLE.getCode(), ex.getCode());
    }

    @Test
    void updatePermissions_invalidPermissionCode_throwsUpdateFailed() {
        BizException ex = assertThrows(BizException.class,
            () -> service.updatePermissions("MERCHANT", List.of("NOT_A_PERMISSION")));
        assertEquals(PlatformCodeEnum.ROLE_PERMISSION_UPDATE_FAILED.getCode(), ex.getCode());
    }

    @Test
    void updatePermissions_validRole_writesRedis() {
        service.updatePermissions("MERCHANT", List.of("GOODS_VIEW", "GOODS_MANAGE"));
        verify(valueOperations).set("platform:role-permissions:MERCHANT", "[\"GOODS_VIEW\",\"GOODS_MANAGE\"]");
    }

    @Test
    void listRoles_returnsAllRolesWithPermissions() {
        List<RoleVO> roles = service.listRoles();
        assertEquals(3, roles.size());
        for (RoleVO vo : roles) {
            assertNotNull(vo.getRoleCode());
            assertFalse(vo.getPermissions().isEmpty());
        }
    }
}
