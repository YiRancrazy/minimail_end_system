package com.yirancrazy.minimall.common.aspect;

import com.yirancrazy.minimall.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 为 service 层和 controller 层方法统一记录开始、结束、耗时及异常日志。
 * Controller 日志额外关联 MDC 中的 traceId，并记录 HTTP 方法和 URI；切面只观察调用，不改变业务返回值。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    // 匹配所有 service 包及其子包（含 service.impl）下的 bean 方法
    /** 匹配任意模块 service 包及其子包中的方法。 */
    @Pointcut("execution(* com.yirancrazy.minimall..service..*.*(..))")
    public void servicePointcut() {}

    /** 匹配任意模块 controller 包及其子包中的方法。 */
    @Pointcut("execution(* com.yirancrazy.minimall..controller..*.*(..))")
    public void controllerPointcut() {}

    /** 匹配标注 Spring {@code @Transactional} 的方法。 */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointcut() {}

    /**
     * 记录 service 方法调用结果与耗时；目标方法抛出的异常会记录完整堆栈后原样抛出。
     *
     * @param joinPoint 被拦截的 service 方法调用
     * @return 目标方法原始返回值
     * @throws Throwable 目标方法执行失败时原样抛出
     */
    @Around("servicePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        long start = System.currentTimeMillis();
        log.info("【开始】 traceId: {}, 方法: {}.{}, 参数: {}",
                        traceId,
                        className,
                        methodName,
                        joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("【结束】 traceId: {}, 方法: {}.{}, 耗时: {}ms", traceId, className, methodName, duration);
            return result;
        } catch (Throwable e) {
            log.error("【异常】 traceId: {}, 方法: {}.{}, 异常信息: {}",
                            traceId, className, methodName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 记录 controller 方法对应的 traceId、HTTP 方法、URI、结果耗时和异常。
     *
     * @param joinPoint 被拦截的 controller 方法调用
     * @return 目标方法原始返回值
     * @throws Throwable 目标方法执行失败时原样抛出
     */
    @Around("controllerPointcut()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // 获取请求属性
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 获取 HTTP 方法和 URI
        String httpMethod = null;
        String uri = null;
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            httpMethod = request.getMethod();
            uri = request.getRequestURI();
        }

        // 从 MDC 取当前链路 traceId（由 TraceIdFilter 写入），便于按链路聚合日志
        String traceId = MDC.get(Result.TRACE_ID_KEY);

        // 记录方法调用信息
        long start = System.currentTimeMillis();
        log.info(
                "【开始】 traceId: {}，方法: {}.{}, HTTP方法: {}, URI: {}",
                traceId,
                className,
                methodName,
                httpMethod,
                uri);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("【结束】 traceId: {}，方法: {}.{}, 耗时: {}ms", traceId, className, methodName, duration);
            return result;
        } catch (Throwable e) {
            log.error(
                    "【异常】 traceId: {}，方法: {}.{}, 异常信息: {}",
                    traceId,
                    className,
                    methodName,
                    e.getMessage(),
                    e);
            throw e;
        }
    }
}
