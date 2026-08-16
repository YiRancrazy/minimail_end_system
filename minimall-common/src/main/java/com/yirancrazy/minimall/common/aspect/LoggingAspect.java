package com.yirancrazy.minimall.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: Service 与 Controller 层方法日志切面，记录方法调用入参、返回结果、耗时、异常信息与 traceId。
 * @Version: 1.0
 * @DateTime: 2026/8/16 8:15
 **/

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    // 匹配所有 service 包及其子包（含 service.impl）下的 bean 方法
    @Pointcut("execution(* com.yirancrazy.minimall..service..*.*(..))")
    public void servicePointcut() {}

    @Pointcut("execution(* com.yirancrazy.minimall..controller..*.*(..))")
    public void controllerPointcut() {}

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalPointcut() {}

    @Around("servicePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long start = System.currentTimeMillis();
        log.info("【开始】方法: {}, 参数: {}", methodName, args);
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("【结束】方法: {}, 返回值: {}, 耗时: {}ms", methodName, result, duration);
            return result;
        }
        catch (Throwable e) {
            log.error("【异常】方法: {}, 异常信息: {}", methodName, e.getMessage(), e);
            throw e;
        }
    }

    @Around("controllerPointcut()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

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
        log.info("【开始】 traceId: {}，方法: {}.{}, 参数: {}, HTTP方法: {}, URI: {}",
                traceId, className, methodName, args, httpMethod, uri);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("【结束】 traceId: {}，方法: {}.{}, 返回值: {}, 耗时: {}ms",
                    traceId, className, methodName, result, duration);
            return result;
        }
        catch (Throwable e) {
            log.error("【异常】 traceId: {}，方法: {}.{}, 异常信息: {}",
                    traceId, className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}
