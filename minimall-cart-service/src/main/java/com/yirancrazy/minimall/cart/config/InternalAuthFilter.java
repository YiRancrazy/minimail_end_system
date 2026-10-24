package com.yirancrazy.minimall.cart.config;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验 cart-service 内部接口访问令牌。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String internalToken;
    private final ObjectMapper objectMapper;

    /**
     * 构造内部接口访问过滤器。
     * @param internalToken 服务间访问令牌
     * @param objectMapper JSON 序列化器
     */
    public InternalAuthFilter(
            @Value("${minimall.security.internal-token:}") String internalToken,
            ObjectMapper objectMapper) {
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    /**
     * 对 /internal/** 请求执行服务级鉴权。
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 处理失败
     * @throws IOException 响应写入失败
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (internalToken.isBlank() || !internalToken.equals(requestToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Result.fail(
                CartCodeEnum.INTERNAL_ACCESS_DENIED.getCode(),
                CartCodeEnum.INTERNAL_ACCESS_DENIED.getMessage()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
