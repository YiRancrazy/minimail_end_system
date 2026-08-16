package com.yirancrazy.minimall.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LoggingAspect 单元测试，验证 service 与 controller 切面在正常返回时透传结果、异常时原样重抛。
 */
class LoggingAspectTest {

    private LoggingAspect aspect;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    void setUp() {
        aspect = new LoggingAspect();
        pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("Target.doWork()");
        when(signature.getDeclaringTypeName()).thenReturn("com.yirancrazy.minimall.common.aspect.Target");
        when(signature.getName()).thenReturn("doWork");
        when(pjp.getSignature()).thenReturn(signature);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logAround_proceedsAndReturnsResult() throws Throwable {
        when(pjp.proceed()).thenReturn("ok");
        assertEquals("ok", aspect.logAround(pjp));
    }

    @Test
    void logAround_rethrowsOriginalException() throws Throwable {
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> aspect.logAround(pjp));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void logAroundController_proceedsAndReturnsResult() throws Throwable {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/v1/test")));
        when(pjp.proceed()).thenReturn("ok");
        assertEquals("ok", aspect.logAroundController(pjp));
    }

    @Test
    void logAroundController_rethrowsOriginalException() throws Throwable {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/v1/test")));
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> aspect.logAroundController(pjp));
        assertEquals("boom", ex.getMessage());
    }
}
