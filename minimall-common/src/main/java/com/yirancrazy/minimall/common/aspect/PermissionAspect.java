package com.yirancrazy.minimall.common.aspect;

import java.util.Collections;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 权限校验切面，拦截 @RequirePermission 注解方法，依据网关注入的 X-User-Role 头进行 RBAC 校验。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final StringRedisTemplate redisTemplate;

    /**
     * 权限切面构造器。
     * @param redisTemplate Redis 模板；为空时无法读取动态权限，受保护操作默认拒绝
     */
    public PermissionAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拦截标注 @RequirePermission 的方法，校验当前请求角色是否具备所需权限。
     * @param pjp 连接点
     * @param requirePermission 权限注解（由参数绑定自动注入）
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常或权限校验失败抛出的 BizException
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        String roleCode = extractRoleCode();
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            log.warn("权限校验失败：未知 role={}", roleCode);
            throw new BizException(CommonCode.FORBIDDEN, "无访问权限");
        }

        PermissionEnum[] required = requirePermission.value();
        String method = pjp.getSignature().toShortString();
        Set<PermissionEnum> effective = resolveEffectivePermissions(role, method);
        boolean authorized = requirePermission.requireAll()
            ? hasAllPermissions(effective, required)
            : hasAnyPermission(effective, required);

        if (!authorized) {
            log.warn("权限校验失败：role={}，required={}", roleCode, required);
            throw new BizException(CommonCode.FORBIDDEN, "无访问权限");
        }
        return pjp.proceed();
    }

    private String extractRoleCode() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(HEADER_USER_ROLE);
    }

    /**
     * 解析角色生效权限集：仅信任 Redis 中的动态覆盖集，读取失败或无覆盖时返回空集。
     * @param role 角色
     * @param method 受保护方法签名
     * @return 生效权限集
     */
    private Set<PermissionEnum> resolveEffectivePermissions(RoleEnum role, String method) {
        if (redisTemplate == null) {
            log.warn("权限缓存不可用，拒绝请求。role={}，method={}，exceptionType={}",
                role.getCode(), method, "RedisTemplateUnavailable");
            return Collections.emptySet();
        }
        String json;
        try {
            json = redisTemplate.opsForValue().get(RolePermissionMapping.KEY_PREFIX + role.getCode());
        }
        catch (Exception e) {
            log.warn("权限缓存读取失败，拒绝请求。role={}，method={}，exceptionType={}",
                role.getCode(), method, e.getClass().getName());
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

    private boolean hasAnyPermission(Set<PermissionEnum> effective, PermissionEnum[] required) {
        for (PermissionEnum p : required) {
            if (effective.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAllPermissions(Set<PermissionEnum> effective, PermissionEnum[] required) {
        for (PermissionEnum p : required) {
            if (!effective.contains(p)) {
                return false;
            }
        }
        return true;
    }
}
