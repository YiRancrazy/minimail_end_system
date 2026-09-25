package com.yirancrazy.minimall.common.aspect;

import java.lang.reflect.Proxy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * PermissionAspect 单元测试，验证权限校验切面对不同角色与权限组合的放行/拒绝逻辑。
 */
class PermissionAspectTest {

    private PermissionAspect aspect;
    private ProceedingJoinPoint pjp;
    private int proceedCount;

    @BeforeEach
    void setUp() throws Throwable {
        // Redis 不可用时鉴权应默认拒绝。
        aspect = new PermissionAspect(null);
        pjp = mock(ProceedingJoinPoint.class);
        proceedCount = 0;
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("PermissionAspectTest.protectedOperation()");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenAnswer(inv -> {
            proceedCount++;
            return null;
        });
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setRoleHeader(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (role != null) {
            request.addHeader("X-User-Role", role);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private RequirePermission annotation(PermissionEnum[] perms, boolean requireAll) {
        return (RequirePermission) Proxy.newProxyInstance(
            RequirePermission.class.getClassLoader(),
            new Class<?>[]{RequirePermission.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "value":
                        return perms;
                    case "requireAll":
                        return requireAll;
                    case "annotationType":
                        return RequirePermission.class;
                    default:
                        return null;
                }
            });
    }

    @Test
    void platform_role_passes_when_has_permission() {
        useRedisPermissions(RoleEnum.PLATFORM, "[\"ROLE_VIEW\"]");
        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
        assertEquals(1, proceedCount);
    }

    @Test
    void user_role_denied_for_platform_permission() {
        useRedisPermissions(RoleEnum.USER, "[\"GOODS_VIEW\"]");
        setRoleHeader("USER");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    @Test
    void unknown_role_denied() {
        setRoleHeader("UNKNOWN");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.GOODS_VIEW}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void missing_role_header_denied() {
        setRoleHeader(null);
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.GOODS_VIEW}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void requireAny_passes_when_role_has_one_of_required() {
        useRedisPermissions(RoleEnum.USER, "[\"ORDER_VIEW\"]");
        setRoleHeader("USER");
        RequirePermission rp = annotation(
            new PermissionEnum[]{PermissionEnum.ORDER_DELETE, PermissionEnum.ORDER_VIEW}, false);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void requireAll_fails_when_role_missing_one() {
        useRedisPermissions(RoleEnum.PLATFORM, "[\"ROLE_VIEW\"]");
        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(
            new PermissionEnum[]{PermissionEnum.ROLE_VIEW, PermissionEnum.CART_MANAGE}, true);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void requireAll_passes_when_role_has_all() {
        useRedisPermissions(RoleEnum.USER, "[\"ORDER_VIEW\",\"CART_MANAGE\"]");
        setRoleHeader("USER");
        RequirePermission rp = annotation(
            new PermissionEnum[]{PermissionEnum.ORDER_VIEW, PermissionEnum.CART_MANAGE}, true);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void merchant_can_view_orders() {
        useRedisPermissions(RoleEnum.MERCHANT, "[\"ORDER_VIEW\"]");
        setRoleHeader("MERCHANT");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ORDER_VIEW}, false);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void merchant_cannot_create_orders() {
        useRedisPermissions(RoleEnum.MERCHANT, "[\"ORDER_VIEW\"]");
        setRoleHeader("MERCHANT");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ORDER_CREATE}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    @Test
    void redis_override_grants_permission_absent_in_static_table() {
        useRedisPermissions(RoleEnum.MERCHANT, "[\"ROLE_VIEW\"]");

        setRoleHeader("MERCHANT");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);

        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
        assertEquals(1, proceedCount);
    }

    @Test
    void redis_override_revokes_static_permission() {
        useRedisPermissions(RoleEnum.PLATFORM, "[\"GOODS_VIEW\"]");

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    @Test
    void redis_template_unavailable_denies_permission() {
        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    @Test
    void redis_missing_override_denies_permission() {
        useRedisPermissions(RoleEnum.PLATFORM, null);

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    @Test
    void redis_empty_override_denies_permission() {
        useRedisPermissions(RoleEnum.PLATFORM, "[]");

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    @Test
    void redis_read_failure_denies_permission() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(RolePermissionMapping.KEY_PREFIX + "PLATFORM"))
            .thenThrow(new RuntimeException("redis unavailable"));
        aspect = new PermissionAspect(redis);

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[]{PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    private void useRedisPermissions(RoleEnum role, String permissions) {
        StringRedisTemplate redis = mockRedisValue(
            RolePermissionMapping.KEY_PREFIX + role.getCode(), permissions);
        aspect = new PermissionAspect(redis);
    }

    private StringRedisTemplate mockRedisValue(String key, String value) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(key)).thenReturn(value);
        return redis;
    }
}
