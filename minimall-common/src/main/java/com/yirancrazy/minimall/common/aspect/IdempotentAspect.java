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
 * @Description: IdempotentAspect 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";
    private static final String HEADER_IDEMPOTENT_KEY = "X-Idempotency-Key";

    private final StringRedisTemplate redisTemplate;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 构造 IdempotentAspect 实例。
     * @param redisTemplate redisTemplate 参数
     */
    public IdempotentAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * 校验业务条件。
     * @param pjp pjp 参数
     * @return 处理结果
     * @throws Throwable 处理失败时抛出
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
            log.warn("幂等键缺失，method={}，跳过幂等校验",
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
     * 转换或解析SpELKey。
     * @param expression expression
     * @param signature signature
     * @param args args
     * @return 处理结果
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
            log.warn("解析幂等 SpEL key 失败，expression={}", expression, e);
            return null;
        }
    }


    /**
     * 转换或解析从Header。
     * @return 处理结果
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
