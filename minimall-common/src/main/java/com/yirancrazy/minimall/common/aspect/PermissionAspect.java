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
 * @Description: PermissionAspect 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final StringRedisTemplate redisTemplate;


    /**
     * 构造 PermissionAspect 实例。
     * @param redisTemplate redisTemplate 参数
     */
    public PermissionAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * 校验业务条件。
     * @param pjp pjp 参数
     * @param requirePermission requirePermission 参数
     * @return 处理结果
     * @throws Throwable 处理失败时抛出
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

    /**
     * 提取角色编码。
     * @return 编号或编码
     */
    private String extractRoleCode() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(HEADER_USER_ROLE);
    }


    /**
     * 转换或解析有效Permissions。
     * @param role 角色
     * @param method method
     * @return 结果列表
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

    /**
     * 校验任一权限。
     * @param effective 有效
     * @param required required
     * @return 是否满足条件
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
     * 校验全部Permissions。
     * @param effective 有效
     * @param required required
     * @return 是否满足条件
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
