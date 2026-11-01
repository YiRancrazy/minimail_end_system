package com.yirancrazy.minimall.common.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mockito 验证幂等切面的键解析、Redis 抢占、重复拒绝和失败释放语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
     * @Description: 测试用订单参数对象，提供 SpEL 表达式可访问的 orderNo 属性。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    static class Dto {
        private final String orderNo;

        /**
         * 创建携带订单号的测试参数。
         *
         * @param orderNo 模拟订单号
         */
        Dto(String orderNo) {
            this.orderNo = orderNo;
        }

        /**
         * 返回测试订单号，供 SpEL 反射读取。
         *
         * @return 订单号
         */
        public String getOrderNo() {
            return orderNo;
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试用目标方法集合，分别覆盖请求头兜底、SpEL 键和字面量键三种注解配置。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    static class Target {
        /**
         * 未声明 key 的目标方法，使切面必须回退读取请求头。
         *
         * @return 固定成功值 ok
         */
        @Idempotent
        public Object headerOnly() {
            return "ok";
        }

        /**
         * 使用参数对象中的订单号作为 SpEL 幂等键。
         *
         * @param dto 包含订单号的测试参数
         * @return 固定成功值 ok
         */
        @Idempotent(key = "#dto.orderNo")
        public Object spelKey(Dto dto) {
            return "ok";
        }

        /**
         * 使用 fixed-key 字面量作为幂等键。
         *
         * @return 固定成功值 ok
         */
        @Idempotent(key = "fixed-key")
        public Object literalKey() {
            return "ok";
        }
    }

    /**
     * 测试目标：为每个用例准备 Redis 操作和切面依赖。
     * 测试场景与前置条件：Mockito 注入 RedisTemplate、ValueOperations、JoinPoint 和签名。
     * 关键步骤：配置通用返回值并创建 IdempotentAspect。预期结果：切面可直接执行且签名可读取。
     */
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Target.headerOnly()");
        aspect = new IdempotentAspect(redisTemplate);
    }

    /**
     * 测试目标：隔离用例之间的 Web 请求上下文。
     * 测试场景与前置条件：每个测试结束后调用。关键步骤：重置 RequestContextHolder。
     * 预期结果：后续用例不会复用前一个请求头。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** * 验证首次请求放行；请求头键k1且抢占成功；调用幂等切面；预期返回ok并写入对应键. */
    @Test
    void testFirstRequestWithHeaderKeyProceeds() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(valueOperations.setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS)).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");
        setHeader("k1");

        Object result = aspect.checkIdempotent(pjp);

        assertEquals("ok", result);
        verify(valueOperations).setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS);
        verify(pjp).proceed();
    }

    /** * 验证重复请求被拒绝；键k1已占用；调用幂等切面；预期返回重复错误且目标不执行. */
    @Test
    void testRepeatRequestThrowsIdempotentKeyReused() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(valueOperations.setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS)).thenReturn(false);
        setHeader("k1");

        BizException ex = assertThrows(BizException.class, () -> aspect.checkIdempotent(pjp));

        assertEquals(CommonCode.IDEMPOTENT_KEY_REUSED, ex.getCode());
        verify(pjp, never()).proceed();
    }

    /** * 验证缺失幂等键拒绝执行；无注解键且无请求头；调用幂等切面；预期返回键缺失错误. */
    @Test
    void testMissingKeyThrowsIdempotentKeyNotExist() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));

        BizException ex = assertThrows(BizException.class, () -> aspect.checkIdempotent(pjp));

        assertEquals(CommonCode.IDEMPOTENT_KEY_NOT_EXIST, ex.getCode());
        verify(pjp, never()).proceed();
    }

    /** * 验证执行失败释放幂等键；键k1抢占成功且目标抛错；调用幂等切面；预期原错传播并删除键. */
    @Test
    void testBusinessFailureReleasesKeyForRetry() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("headerOnly"));
        when(valueOperations.setIfAbsent("idempotent:k1", "1", 24L, TimeUnit.HOURS)).thenReturn(true);
        when(pjp.proceed()).thenThrow(new RuntimeException("boom"));
        setHeader("k1");

        assertThrows(RuntimeException.class, () -> aspect.checkIdempotent(pjp));

        verify(redisTemplate).delete("idempotent:k1");
    }

    /** * 验证SpEL键解析；参数订单号为ON-001；调用幂等切面；预期使用订单号对应键. */
    @Test
    void testSpelKeyResolvesFromMethodArg() throws Throwable {
        Dto dto = new Dto("ON-001");
        when(pjp.getArgs()).thenReturn(new Object[] {dto});
        when(signature.getMethod()).thenReturn(Target.class.getMethod("spelKey", Dto.class));
        when(valueOperations.setIfAbsent("idempotent:ON-001", "1", 24L, TimeUnit.HOURS))
                .thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.checkIdempotent(pjp);

        verify(valueOperations).setIfAbsent("idempotent:ON-001", "1", 24L, TimeUnit.HOURS);
    }

    /** * 验证字面量键解析；注解键为fixed-key；调用幂等切面；预期使用固定键值. */
    @Test
    void testLiteralKeyUsesLiteralValue() throws Throwable {
        when(signature.getMethod()).thenReturn(Target.class.getMethod("literalKey"));
        when(valueOperations.setIfAbsent("idempotent:fixed-key", "1", 24L, TimeUnit.HOURS))
                .thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        aspect.checkIdempotent(pjp);

        verify(valueOperations).setIfAbsent("idempotent:fixed-key", "1", 24L, TimeUnit.HOURS);
    }

    /**
     * 测试目标：为测试请求设置稳定的幂等请求头。
     *
     * 测试场景与前置条件：测试需要模拟 Web 请求头兜底。
     * 关键步骤：创建 Mock 请求、写入指定键并绑定 RequestContextHolder。
     * 预期结果：切面可从当前线程读取该请求头。
     *
     * @param value 幂等键请求头值
     */
    private void setHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, value);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
