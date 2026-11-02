package com.yirancrazy.minimall.common.aspect;

import com.yirancrazy.minimall.common.annotation.Idempotency;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import com.yirancrazy.minimall.common.registry.IdempotencyReplayHandlerRegistry;
import com.yirancrazy.minimall.common.result.CommonCode;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 拦截标注 {@link Idempotency} 的方法，解析稳定幂等键并通过 Redis SETNX 抢占执行权。
 * 首次执行发生异常时会删除键以允许同键重试；重复请求直接抛出业务异常。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Aspect
@Component
public class IdempotencyAspect {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final String ATTR_IDEMPOTENCY_KEY = "idempotencyKey";
    private static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";
    // 模板键中的变量占位符，如 order:checkout:#userId:#idempotencyKey
    private static final Pattern SPEL_TOKEN = Pattern.compile("#\\w+(?:\\.\\w+)*");

    private final StringRedisTemplate redisTemplate;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();
    private final IdempotencyReplayHandlerRegistry idempotencyReplayHandlerRegistry;
    // IdempotencyAspect.java 关键部分
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class);

    /**
     * 创建幂等切面并注入 Redis 操作模板。
     *
     * @param redisTemplate 用于保存幂等键的 Redis 模板，必须支持字符串操作
     */
    public IdempotencyAspect(
            StringRedisTemplate redisTemplate,
            IdempotencyReplayHandlerRegistry idempotencyReplayHandlerRegistry) {
        this.redisTemplate = redisTemplate;
        this.idempotencyReplayHandlerRegistry = idempotencyReplayHandlerRegistry;
    }

    /**
     * 在目标方法执行前解析并抢占幂等键，成功后执行目标方法。
     * 目标方法抛出异常时释放本次占用的键，使调用方可以使用同一键重试。
     *
     * @param pjp 被拦截的目标方法调用
     * @return 目标方法返回值
     * @throws Throwable 幂等键缺失、重复请求或目标方法执行失败时抛出
     */
    @Around("@annotation(com.yirancrazy.minimall.common.annotation.Idempotency)")
    public Object checkIdempotency(ProceedingJoinPoint pjp) throws Throwable {

        // 获取方法签名
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Idempotency idempotent = method.getAnnotation(Idempotency.class);

        // 从方法注解中获取幂等键
        String key = resolveSpelKey(idempotent.key(), signature, pjp.getArgs());

        // 如果方法注解中没有定义幂等键，则从请求头中获取
        if (key == null || key.isBlank()) {
            key = resolveFromHeader();
        }

        // 如果仍然没有获取到幂等键，则拒绝执行
        if (key == null || key.isBlank()) {
            log.warn("幂等键缺失，method={}，拒绝执行", pjp.getSignature().toShortString());
            throw new BizException(CommonCode.IDEMPOTENCY_KEY_NOT_EXIST, "幂等键缺失");
        }

        RequestContextHolder.currentRequestAttributes()
                .setAttribute(ATTR_IDEMPOTENCY_KEY, key, RequestAttributes.SCOPE_REQUEST);

        String redisToken = UUID.randomUUID().toString();
        // 生成redis 键
        String redisKey = IDEMPOTENCY_KEY_PREFIX + key;
        Boolean acquired =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(redisKey, redisToken, idempotent.expire(), idempotent.unit());
        if (Boolean.TRUE.equals(acquired)) {
            try {
                return pjp.proceed();
            } catch (Throwable t) {
                // 首次执行失败，释放幂等键允许客户端用同键重试
                releaseIfOwner(redisKey, redisToken);
                throw t;
            }
        }

        // 尝试重放请求
        IdempotencyReplayHandler handler = idempotencyReplayHandlerRegistry.find(idempotent.replayType());

        Object replayed = handler == null
                ? null
                : handler.onDuplicate(new IdempotencyContext(method, pjp.getArgs(), key));

        if (replayed != null) {
            return replayed;
        }

        throw new BizException(CommonCode.IDEMPOTENCY_KEY_REUSED, "重复请求，请勿重复提交");
    }

    /**
     * 将注解中的幂等键配置解析为实际键值。空表达式返回 null；
     * 不含 {@code #} 时按字面量返回；以 {@code #} 开头时整体按 SpEL 求值；
     * 其他含 {@code #} 的内容按模板处理，将内嵌变量替换为求值结果。
     *
     * @param expression 注解中声明的字面量、SpEL 表达式或模板表达式
     * @param signature 目标方法签名，用于读取参数名
     * @param args 目标方法实参，顺序必须与参数名一致
     * @return 解析后的幂等键；表达式为空、求值失败或结果为 null 时返回 null
     */
    private String resolveSpelKey(String expression, MethodSignature signature, Object[] args) {
        // 如果表达式为空
        if (expression == null || expression.isBlank()) {
            return null;
        }
        // 不含变量占位符，按字面量处理
        if (expression.indexOf('#') < 0) {
            return expression;
        }
        try {
            EvaluationContext context = buildContext(signature, args);
            // 以 # 开头时整体求值，支持 #dto.orderNo 等属性路径
            if (expression.startsWith("#")) {
                Object value = spelParser.parseExpression(expression).getValue(context);
                return value == null ? null : String.valueOf(value);
            }
            // 其余按模板处理，如 order:checkout:#userId:#idempotencyKey
            return interpolate(expression, context);
        } catch (Exception e) {
            log.warn("解析幂等 SpEL key 失败，expression={}", expression, e);
            return null;
        }
    }

    /**
     * 绑定方法参数名与实参，创建 SpEL 求值上下文。
     *
     * @param signature 目标方法签名，用于读取参数名
     * @param args 目标方法实参，顺序必须与参数名一致
     * @return 已绑定方法参数的求值上下文
     */
    private EvaluationContext buildContext(MethodSignature signature, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        return context;
    }

    /**
     * 将模板中的 {@code #变量} 替换为对应 SpEL 求值结果。
     *
     * @param template 字面量前缀中嵌入变量的模板表达式
     * @param context 已绑定方法参数的求值上下文
     * @return 变量替换后的键值
     */
    private String interpolate(String template, EvaluationContext context) {
        Matcher matcher = SPEL_TOKEN.matcher(template);
        StringBuilder resolved = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            resolved.append(template, last, matcher.start());
            Object value = spelParser.parseExpression(matcher.group()).getValue(context);
            resolved.append(value == null ? "" : value);
            last = matcher.end();
        }
        resolved.append(template, last, template.length());
        return resolved.toString();
    }

    /**
     * 从当前 Servlet 请求的 X-Idempotency-Key 请求头读取兜底幂等键。
     *
     * @return 请求头值；当前线程没有 Web 请求上下文时返回 null
     */
    private String resolveFromHeader() {
        // 从 Spring 的当前线程请求上下文中，取出当前 HTTP 请求的属性对象，并强转为 Servlet 专用的 ServletRequestAttributes，以便获取请求头信息。
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getHeader(HEADER_IDEMPOTENCY_KEY);
    }

    /**
     * 释放指定的幂等键，允许重试。
     *
     * @param redisKey redis 键
     * @param token 释放令牌,判断锁是否属于当前线程，避免误释放
     */
    private void releaseIfOwner(String redisKey, String token) {
        redisTemplate.execute(RELEASE_SCRIPT, List.of(redisKey), token);
    }
}
