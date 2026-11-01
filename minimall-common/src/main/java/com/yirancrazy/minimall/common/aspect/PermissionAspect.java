package com.yirancrazy.minimall.common.aspect;

import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 拦截标注 {@link RequirePermission} 的方法，根据 X-User-Role 与 Redis 权限快照
 * 校验角色是否具备所需权限。权限缓存缺失或读取失败时按拒绝访问处理。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final StringRedisTemplate redisTemplate;

    /**
     * 创建权限切面并注入可选的 Redis 权限存储。
     *
     * @param redisTemplate 保存角色权限覆盖配置的 Redis 模板；可为 null，此时所有鉴权请求都会被拒绝
     */
    public PermissionAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 校验当前请求角色是否满足方法声明的权限要求，通过后执行目标方法。
     * 角色未知、权限不足或缓存不可用时抛出无访问权限异常。
     *
     * @param pjp 被拦截的目标方法调用
     * @param requirePermission 当前方法声明的权限要求
     * @return 目标方法返回值
     * @throws Throwable 鉴权失败或目标方法执行失败时抛出
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission)
            throws Throwable {
        String roleCode = extractRoleCode();
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            log.warn("权限校验失败：未知 role={}", roleCode);
            throw new BizException(CommonCode.FORBIDDEN, "无访问权限");
        }

        PermissionEnum[] required = requirePermission.value();
        String method = pjp.getSignature().toShortString();
        Set<PermissionEnum> effective = resolveEffectivePermissions(role, method);
        boolean authorized =
                requirePermission.requireAll()
                        ? hasAllPermissions(effective, required)
                        : hasAnyPermission(effective, required);

        if (!authorized) {
            log.warn("权限校验失败：role={}，required={}", roleCode, required);
            throw new BizException(CommonCode.FORBIDDEN, "无访问权限");
        }
        return pjp.proceed();
    }

    /**
     * 从当前 HTTP 请求头 X-User-Role 读取角色编码。
     *
     * @return 角色编码；没有 Web 请求上下文时返回 null
     */
    private String extractRoleCode() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(HEADER_USER_ROLE);
    }

    /**
     * 读取指定角色的 Redis 权限覆盖配置并解析为权限集合。
     * Redis 未注入、读取失败、键缺失或解析结果为空时返回空集合，调用方应拒绝请求。
     *
     * @param role 已解析的请求角色
     * @param method 当前方法标识，仅用于记录拒绝原因
     * @return 不可变的有效权限集合；缓存不可用时为空集合
     */
    private Set<PermissionEnum> resolveEffectivePermissions(RoleEnum role, String method) {
        if (redisTemplate == null) {
            log.warn(
                    "权限缓存不可用，拒绝请求。role={}，method={}，exceptionType={}",
                    role.getCode(),
                    method,
                    "RedisTemplateUnavailable");
            return Collections.emptySet();
        }
        String json;
        try {
            json = redisTemplate.opsForValue().get(RolePermissionMapping.KEY_PREFIX + role.getCode());
        } catch (Exception e) {
            log.warn(
                    "权限缓存读取失败，拒绝请求。role={}，method={}，exceptionType={}",
                    role.getCode(),
                    method,
                    e.getClass().getName());
            return Collections.emptySet();
        }
        if (json == null || json.isEmpty()) {
            log.warn("权限覆盖配置缺失，拒绝请求。role={}，method={}", role.getCode(), method);
            return Collections.emptySet();
        }
        Set<PermissionEnum> permissions = RolePermissionMapping.parsePermissions(json);
        if (permissions.isEmpty()) {
            log.warn("权限覆盖配置为空，拒绝请求。role={}，method={}", role.getCode(), method);
        }
        return permissions;
    }

    /**
     * 判断有效权限是否至少包含一个必需权限。
     *
     * @param effective 角色当前拥有的权限
     * @param required 方法声明的权限数组
     * @return 命中任一必需权限时为 true
     */
    private boolean hasAnyPermission(Set<PermissionEnum> effective, PermissionEnum[] required) {
        for (PermissionEnum p : required) {
            if (effective.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断有效权限是否包含方法声明的全部权限。
     *
     * @param effective 角色当前拥有的权限
     * @param required 方法声明的权限数组
     * @return 全部必需权限均存在时为 true；空数组按 true 处理
     */
    private boolean hasAllPermissions(Set<PermissionEnum> effective, PermissionEnum[] required) {
        for (PermissionEnum p : required) {
            if (!effective.contains(p)) {
                return false;
            }
        }
        return true;
    }
}
