package com.yirancrazy.minimall.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
            log.warn("permission denied: unknown role={}", roleCode);
            throw new BizException(CommonCode.FORBIDDEN, "无访问权限");
        }

        PermissionEnum[] required = requirePermission.value();
        boolean authorized;
        if (requirePermission.requireAll()) {
            authorized = hasAllPermissions(role, required);
        }
        else {
            authorized = hasAnyPermission(role, required);
        }

        if (!authorized) {
            log.warn("permission denied: role={}, required={}", roleCode, required);
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

    private boolean hasAnyPermission(RoleEnum role, PermissionEnum[] required) {
        for (PermissionEnum p : required) {
            if (RolePermissionMapping.hasPermission(role, p)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAllPermissions(RoleEnum role, PermissionEnum[] required) {
        for (PermissionEnum p : required) {
            if (!RolePermissionMapping.hasPermission(role, p)) {
                return false;
            }
        }
        return true;
    }
}
