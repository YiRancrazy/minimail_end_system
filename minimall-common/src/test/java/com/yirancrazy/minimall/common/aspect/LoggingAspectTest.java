package com.yirancrazy.minimall.common.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证日志切面对 service 与 controller 调用的透传、异常传播和请求上下文依赖。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class LoggingAspectTest {

    private LoggingAspect aspect;
    private ProceedingJoinPoint pjp;

    /**
     * 测试目标：为每个用例准备可复用的切面和 JoinPoint 签名。
     * 测试场景与前置条件：Mock 方法签名和目标调用。关键步骤：配置声明类、方法名和短签名。
     * 预期结果：日志切面可读取稳定方法标识。
     */
    @BeforeEach
    void setUp() {
        aspect = new LoggingAspect();
        pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("Target.doWork()");
        when(signature.getDeclaringTypeName())
                .thenReturn("com.yirancrazy.minimall.common.aspect.Target");
        when(signature.getName()).thenReturn("doWork");
        when(pjp.getSignature()).thenReturn(signature);
    }

    /**
     * 测试目标：隔离请求上下文。
     * 测试场景与前置条件：每个测试结束后调用。关键步骤：清空 RequestContextHolder。
     * 预期结果：后续用例不会继承 Mock 请求。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** * 验证服务切面透传结果；目标调用返回ok；执行日志切面；预期仍返回ok. */
    @Test
    void testLogAroundProceedsAndReturnsResult() throws Throwable {
        when(pjp.proceed()).thenReturn("ok");
        assertEquals("ok", aspect.logAround(pjp));
    }

    /** * 验证服务切面保留异常；目标调用抛状态异常；执行日志切面；预期同类同消息异常向上抛出. */
    @Test
    void testLogAroundRethrowsOriginalException() throws Throwable {
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> aspect.logAround(pjp));
        assertEquals("boom", ex.getMessage());
    }

    /** * 验证控制器切面透传结果；绑定GET请求且调用返回ok；执行日志切面；预期仍返回ok. */
    @Test
    void testLogAroundControllerProceedsAndReturnsResult() throws Throwable {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/v1/test")));
        when(pjp.proceed()).thenReturn("ok");
        assertEquals("ok", aspect.logAroundController(pjp));
    }

    /** * 验证控制器切面保留异常；绑定GET请求且调用抛状态异常；执行日志切面；预期原异常传播. */
    @Test
    void testLogAroundControllerRethrowsOriginalException() throws Throwable {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/v1/test")));
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> aspect.logAroundController(pjp));
        assertEquals("boom", ex.getMessage());
    }
}
