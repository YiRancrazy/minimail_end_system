package com.yirancrazy.minimall.common.aspect;

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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证权限切面在角色识别、任一/全部权限、Redis 覆盖和缓存故障下的放行或拒绝行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PermissionAspectTest {

    private PermissionAspect aspect;
    private ProceedingJoinPoint pjp;
    private int proceedCount;

    /**
     * 测试目标：为每个用例准备默认拒绝的权限切面和可计数目标方法。
     *
     * 测试场景与前置条件：切面默认不注入 Redis，JoinPoint.proceed 被调用时递增计数。
     * 关键步骤：创建 Mock 签名与动态代理目标调用。
     * 预期结果：默认状态可用于验证 Redis 缺失时的拒绝行为。
     *
     * @throws Throwable 配置 Mockito 行为时若发生错误则抛出
     */
    @BeforeEach
    void setUp() throws Throwable {
        // Redis 不可用时鉴权应默认拒绝。
        aspect = new PermissionAspect(null);
        pjp = mock(ProceedingJoinPoint.class);
        proceedCount = 0;
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("PermissionAspectTest.protectedOperation()");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed())
                .thenAnswer(
                        inv -> {
                            proceedCount++;
                            return null;
                        });
    }

    /**
     * 测试目标：隔离测试之间的请求上下文。
     * 测试场景与前置条件：每个测试结束后调用。关键步骤：重置 RequestContextHolder。
     * 预期结果：后续用例不会继承角色请求头。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 在测试线程中设置当前角色请求头。
     *
     * @param role 角色编码；null 表示不设置 X-User-Role
     */
    private void setRoleHeader(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (role != null) {
            request.addHeader("X-User-Role", role);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /**
     * 创建最小化的 RequirePermission 动态代理，以便单元测试直接控制权限数组和匹配模式。
     *
     * @param perms 注解声明的权限数组
     * @param requireAll 是否要求全部权限
     * @return 可在切面中读取的注解代理
     */
    private RequirePermission annotation(PermissionEnum[] perms, boolean requireAll) {
        return (RequirePermission)
                Proxy.newProxyInstance(
                        RequirePermission.class.getClassLoader(),
                        new Class<?>[] {RequirePermission.class},
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

    /** * 验证平台角色放行；拥有角色查看权限；执行权限切面；预期目标执行一次. */
    @Test
    void testPlatformRolePassesWhenHasPermission() {
        useRedisPermissions(RoleEnum.PLATFORM, "[\"ROLE_VIEW\"]");
        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
        assertEquals(1, proceedCount);
    }

    /** * 验证用户越权拒绝；仅有商品查看权限；执行权限切面；预期抛业务异常且目标不执行. */
    @Test
    void testUserRoleDeniedForPlatformPermission() {
        useRedisPermissions(RoleEnum.USER, "[\"GOODS_VIEW\"]");
        setRoleHeader("USER");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    /** * 验证未知角色拒绝；请求头角色为未知值；执行权限切面；预期抛业务异常. */
    @Test
    void testUnknownRoleDenied() {
        setRoleHeader("UNKNOWN");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.GOODS_VIEW}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证缺失角色拒绝；请求不含角色头；执行权限切面；预期抛业务异常. */
    @Test
    void testMissingRoleHeaderDenied() {
        setRoleHeader(null);
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.GOODS_VIEW}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证任一权限放行；用户拥有订单查看权限；执行权限切面；预期不抛异常. */
    @Test
    void testRequireAnyPassesWhenRoleHasOneOfRequired() {
        useRedisPermissions(RoleEnum.USER, "[\"ORDER_VIEW\"]");
        setRoleHeader("USER");
        RequirePermission rp =
                annotation(
                        new PermissionEnum[] {PermissionEnum.ORDER_DELETE, PermissionEnum.ORDER_VIEW}, false);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证全部权限拒绝；平台缺少购物车权限；执行权限切面；预期抛业务异常. */
    @Test
    void testRequireAllFailsWhenRoleMissingOne() {
        useRedisPermissions(RoleEnum.PLATFORM, "[\"ROLE_VIEW\"]");
        setRoleHeader("PLATFORM");
        RequirePermission rp =
                annotation(
                        new PermissionEnum[] {PermissionEnum.ROLE_VIEW, PermissionEnum.CART_MANAGE}, true);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证全部权限放行；用户拥有两项所需权限；执行权限切面；预期不抛异常. */
    @Test
    void testRequireAllPassesWhenRoleHasAll() {
        useRedisPermissions(RoleEnum.USER, "[\"ORDER_VIEW\",\"CART_MANAGE\"]");
        setRoleHeader("USER");
        RequirePermission rp =
                annotation(
                        new PermissionEnum[] {PermissionEnum.ORDER_VIEW, PermissionEnum.CART_MANAGE}, true);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证商家查看订单；拥有订单查看权限；执行权限切面；预期不抛异常. */
    @Test
    void testMerchantCanViewOrders() {
        useRedisPermissions(RoleEnum.MERCHANT, "[\"ORDER_VIEW\"]");
        setRoleHeader("MERCHANT");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ORDER_VIEW}, false);
        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证商家不能下单；仅有订单查看权限；执行权限切面；预期抛业务异常. */
    @Test
    void testMerchantCannotCreateOrders() {
        useRedisPermissions(RoleEnum.MERCHANT, "[\"ORDER_VIEW\"]");
        setRoleHeader("MERCHANT");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ORDER_CREATE}, false);
        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
    }

    /** * 验证覆盖授予权限；商家覆盖含角色查看权限；执行权限切面；预期目标执行一次. */
    @Test
    void testRedisOverrideGrantsPermissionAbsentInStaticTable() {
        useRedisPermissions(RoleEnum.MERCHANT, "[\"ROLE_VIEW\"]");

        setRoleHeader("MERCHANT");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);

        assertDoesNotThrow(() -> aspect.checkPermission(pjp, rp));
        assertEquals(1, proceedCount);
    }

    /** * 验证覆盖撤销权限；平台覆盖不含角色查看权限；执行权限切面；预期拒绝且目标不执行. */
    @Test
    void testRedisOverrideRevokesStaticPermission() {
        useRedisPermissions(RoleEnum.PLATFORM, "[\"GOODS_VIEW\"]");

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    /** * 验证Redis缺失拒绝；权限模板为null；执行权限切面；预期抛业务异常. */
    @Test
    void testRedisTemplateUnavailableDeniesPermission() {
        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    /** * 验证权限键缺失拒绝；Redis返回null；执行权限切面；预期抛业务异常. */
    @Test
    void testRedisMissingOverrideDeniesPermission() {
        useRedisPermissions(RoleEnum.PLATFORM, null);

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    /** * 验证空覆盖拒绝；覆盖值为空数组；执行权限切面；预期抛业务异常. */
    @Test
    void testRedisEmptyOverrideDeniesPermission() {
        useRedisPermissions(RoleEnum.PLATFORM, "[]");

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    /** * 验证Redis异常拒绝；读取权限抛运行异常；执行权限切面；预期抛业务异常. */
    @Test
    void testRedisReadFailureDeniesPermission() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(RolePermissionMapping.KEY_PREFIX + "PLATFORM"))
                .thenThrow(new RuntimeException("redis unavailable"));
        aspect = new PermissionAspect(redis);

        setRoleHeader("PLATFORM");
        RequirePermission rp = annotation(new PermissionEnum[] {PermissionEnum.ROLE_VIEW}, false);

        assertThrows(BizException.class, () -> aspect.checkPermission(pjp, rp));
        assertEquals(0, proceedCount);
    }

    /**
     * 使用指定角色和权限 JSON 重建切面，模拟 Redis 覆盖配置。
     *
     * @param role 需要配置的角色
     * @param permissions Redis 中保存的权限 JSON，可为 null
     */
    private void useRedisPermissions(RoleEnum role, String permissions) {
        StringRedisTemplate redis =
                mockRedisValue(RolePermissionMapping.KEY_PREFIX + role.getCode(), permissions);
        aspect = new PermissionAspect(redis);
    }

    /**
     * 创建对指定权限键返回固定值的 Redis Mock。
     *
     * @param key 权限缓存键
     * @param value 缓存值
     * @return 配置完成的 RedisTemplate Mock
     */
    private StringRedisTemplate mockRedisValue(String key, String value) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(key)).thenReturn(value);
        return redis;
    }
}
