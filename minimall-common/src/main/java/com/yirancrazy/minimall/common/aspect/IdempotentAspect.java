package com.yirancrazy.minimall.common.aspect;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 幂等校验切面，拦截 @Idempotent 注解方法，基于 Redis SETNX 做写接口防重放。
 *               幂等键优先取注解 SpEL 表达式（如 #dto.orderNo），否则回退请求头 X-Idempotency-Key
 *               （网关已强制写请求携带）；两者均缺失时跳过不阻塞（直连请求场景）。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";
    private static final String HEADER_IDEMPOTENT_KEY = "X-Idempotency-Key";

    private final StringRedisTemplate redisTemplate;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public IdempotentAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 幂等拦截：同一幂等键在 TTL 内重复提交直接抛幂等错误；业务失败时释放键允许同键重试。
     * @param pjp 连接点
     * @return 目标方法返回值
     * @throws Throwable 目标方法异常或幂等键重复抛出的 BizException
     */
    @Around("@annotation(com.yirancrazy.minimall.common.annotation.Idempotent)")
    public Object checkIdempotent(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        String key = resolveSpelKey(idempotent.key(), signature, pjp.getArgs());
        if (key == null || key.isBlank()) {
            key = resolveFromHeader();
        }
        if (key == null || key.isBlank()) {
            // 未走网关的直连请求无幂等键，幂等检查不阻塞业务
            log.warn("idempotency key missing, method={}, skip idempotent check",
                pjp.getSignature().toShortString());
            return pjp.proceed();
        }

        String redisKey = IDEMPOTENT_KEY_PREFIX + key;
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(redisKey, "1", idempotent.expire(), idempotent.unit());
        if (Boolean.TRUE.equals(acquired)) {
            try {
                return pjp.proceed();
            }
            catch (Throwable t) {
                // 首次执行失败（事务回滚后无副作用），释放幂等键允许客户端用同键重试
                redisTemplate.delete(redisKey);
                throw t;
            }
        }
        throw new BizException(CommonCode.IDEMPOTENT_KEY_REUSED, "重复请求，请勿重复提交");
    }

    /**
     * 按注解 key 表达式解析幂等键：以 # 开头视为 SpEL 从方法参数取值，否则视为字面量。
     * @param expression 注解 key 表达式
     * @param signature 方法签名
     * @param args 方法实参
     * @return 解析后的幂等键；空表达式或解析失败返回 null
     */
    private String resolveSpelKey(String expression, MethodSignature signature, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        if (!expression.startsWith("#")) {
            return expression;
        }
        try {
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Object value = spelParser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        }
        catch (Exception e) {
            log.warn("resolve idempotent SpEL key failed, expression={}", expression, e);
            return null;
        }
    }

    /**
     * 从当前请求头解析幂等键。
     * @return X-Idempotency-Key 值；非 Web 请求上下文时返回 null
     */
    private String resolveFromHeader() {
        // 从 Spring 的当前线程请求上下文中，取出当前 HTTP 请求的属性对象，并强转为 Servlet 专用的 ServletRequestAttributes，以便获取请求头信息。
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getHeader(HEADER_IDEMPOTENT_KEY);
    }
}
