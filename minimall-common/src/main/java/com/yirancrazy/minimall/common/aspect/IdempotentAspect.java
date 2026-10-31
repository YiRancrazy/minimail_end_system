package com.yirancrazy.minimall.common.aspect;

import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 拦截标注 {@link Idempotent} 的方法，解析稳定幂等键并通过 Redis SETNX 抢占执行权。
 * 首次执行发生异常时会删除键以允许同键重试；重复请求直接抛出业务异常。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

  private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";
  private static final String HEADER_IDEMPOTENT_KEY = "X-Idempotency-Key";

  private final StringRedisTemplate redisTemplate;
  private final SpelExpressionParser spelParser = new SpelExpressionParser();
  private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();

  /**
   * 创建幂等切面并注入 Redis 操作模板。
   *
   * @param redisTemplate 用于保存幂等键的 Redis 模板，必须支持字符串操作
   */
  public IdempotentAspect(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 在目标方法执行前解析并抢占幂等键，成功后执行目标方法。
   * 目标方法抛出异常时释放本次占用的键，使调用方可以使用同一键重试。
   *
   * @param pjp 被拦截的目标方法调用
   * @return 目标方法返回值
   * @throws Throwable 幂等键缺失、重复请求或目标方法执行失败时抛出
   */
  @Around("@annotation(com.yirancrazy.minimall.common.annotation.Idempotent)")
  public Object checkIdempotent(ProceedingJoinPoint pjp) throws Throwable {

    // 获取方法签名
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();
    Idempotent idempotent = method.getAnnotation(Idempotent.class);

    // 从方法注解中获取幂等键
    String key = resolveSpelKey(idempotent.key(), signature, pjp.getArgs());

    // 如果方法注解中没有定义幂等键，则从请求头中获取
    if (key == null || key.isBlank()) {
      key = resolveFromHeader();
    }

    // 如果仍然没有获取到幂等键，则拒绝执行
    if (key == null || key.isBlank()) {
      log.warn("幂等键缺失，method={}，拒绝执行", pjp.getSignature().toShortString());
      throw new BizException(CommonCode.IDEMPOTENT_KEY_NOT_EXIST, "幂等键缺失");
    }

    // 生成redis 键
    String redisKey = IDEMPOTENT_KEY_PREFIX + key;
    Boolean acquired =
        redisTemplate
            .opsForValue()
            .setIfAbsent(redisKey, "1", idempotent.expire(), idempotent.unit());
    if (Boolean.TRUE.equals(acquired)) {
      try {
        return pjp.proceed();
      } catch (Throwable t) {
        // 首次执行失败（事务回滚后无副作用），释放幂等键允许客户端用同键重试
        redisTemplate.delete(redisKey);
        throw t;
      }
    }
    throw new BizException(CommonCode.IDEMPOTENT_KEY_REUSED, "重复请求，请勿重复提交");
  }

  /**
   * 将注解中的幂等键配置解析为实际键值。空表达式返回 null；
   * 以 {@code #} 开头的表达式按方法参数名绑定后通过 SpEL 求值，其余内容按字面量返回。
   *
   * @param expression 注解中声明的 SpEL 表达式或字面量
   * @param signature 目标方法签名，用于读取参数名
   * @param args 目标方法实参，顺序必须与参数名一致
   * @return 解析后的幂等键；表达式为空、求值失败或结果为 null 时返回 null
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
    } catch (Exception e) {
      log.warn("解析幂等 SpEL key 失败，expression={}", expression, e);
      return null;
    }
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
    return attrs.getRequest().getHeader(HEADER_IDEMPOTENT_KEY);
  }
}
