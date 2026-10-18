package com.yirancrazy.minimall.common.aspect;

import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdempotentAspectTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotentAspectTest {

    private static final String HEADER = "X-Idempotency-Key";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ProceedingJoinPoint pjp;
    @Mock private MethodSignature signature;

    private IdempotentAspect aspect;

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: Dto 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    static class Dto {
        private final String orderNo;

        Dto(String orderNo) {
            this.orderNo = orderNo;
        }

        /**
         * 获取相关数据。
         * @return 处理结果
         */
        public String getOrderNo() {
            return orderNo;
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: Target 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    static class Target {
        /**
         * 执行 headerOnly 方法。
         * @return 处理结果
         */
        @Idempotent
        public Object headerOnly() {
            return "ok";
        }

        /**
         * 执行 spelKey 方法。
         * @param dto dto 参数
         * @return 处理结果
         */
        @Idempotent(key = "#dto.orderNo")
        public Object spelKey(Dto dto) {
            return "ok";
        }

        /**
         * 执行 literalKey 方法。
         * @return 处理结果
         */
        @Idempotent(key = "fixed-key")
        public Object literalKey() {
            return "ok";
        }
    }

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Target.headerOnly()");
        aspect = new IdempotentAspect(redisTemplate);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void firstRequest_withHeaderKey_proceeds() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(valueOperations.setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS)).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");
        setHeader("k1");

        Object result = aspect.checkIdempotent(pjp);

        assertEquals("ok", result);
        verify(valueOperations).setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS);
        verify(pjp).proceed();
    }

    @Test
    void repeatRequest_throwsIdempotentKeyReused() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(valueOperations.setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS)).thenReturn(false);
        setHeader("k1");

        BizException ex = assertThrows(BizException.class, () -> aspect.checkIdempotent(pjp));

        assertEquals(CommonCode.IDEMPOTENT_KEY_REUSED, ex.getCode());
        verify(pjp, never()).proceed();
    }

    @Test
    void missingKey_skipsCheckAndProceeds() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.checkIdempotent(pjp);

        assertEquals("ok", result);
        verify(redisTemplate, never()).delete("idempotent:k1");
    }

    @Test
    void businessFailure_releasesKeyForRetry() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(valueOperations.setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS)).thenReturn(true);
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));
        setHeader("k1");

        assertThrows(RuntimeException.class, () -> aspect.checkIdempotent(pjp));

        verify(redisTemplate).delete("idempotent:k1");
    }

    @Test
    void spelKey_resolvesFromMethodArg() throws Throwable {
        Dto dto = new Dto("ON-001");
        when(pjp.getArgs()).thenReturn(new Object[]{dto});
        when(signature.getMethod()).thenReturn(Target.class.getMethod("spelKey", Dto.class));
        when(valueOperations.setIfAbsent("idempotent:ON-001", "1", 24L, TimeUnit.HOURS)).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.checkIdempotent(pjp);

        verify(valueOperations).setIfAbsent("idempotent:ON-001", "1", 24L, TimeUnit.HOURS);
    }

    @Test
    void literalKey_usesLiteralValue() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("literalKey"));
        when(valueOperations.setIfAbsent("idempotent:fixed-key", "1", 24L, TimeUnit.HOURS)).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.checkIdempotent(pjp);

        verify(valueOperations).setIfAbsent("idempotent:fixed-key", "1", 24L, TimeUnit.HOURS);
    }

    private void setHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, value);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
