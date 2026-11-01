package com.yirancrazy.minimall.cart.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 X-Internal-Token 保护 /internal/** 下的购物车内部接口，同时允许公开购物车
 *     路径直接通过。过滤器依赖配置的内部令牌和 ObjectMapper；令牌未配置或校验失败时返回
 *     HTTP 403 JSON Result，并且不继续调用后续过滤器链。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String internalToken;
    private final ObjectMapper objectMapper;

    /**
     * 创建依赖内部调用令牌和 JSON 序列化器的访问过滤器.
     *
     * @param internalToken 配置的共享令牌；为空时禁止所有内部接口访问
     * @param objectMapper 用于写入统一禁止访问响应的 JSON 序列化器
     */
    public InternalAuthFilter(
            @Value("${minimall.security.internal-token:}") String internalToken,
            ObjectMapper objectMapper) {
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    /**
     * 对购物车内部请求执行服务级鉴权.
     *
     * <p>公开路径直接放行；内部路径仅在配置令牌非空且与请求头完全一致时继续执行。否则写入
     * HTTP 403 JSON Result，并终止当前请求的过滤器链。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续 Servlet 过滤器链
     * @throws ServletException 后续过滤器处理失败时抛出
     * @throws IOException 禁止访问响应写入失败时抛出
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestToken = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (internalToken.isBlank() || !internalToken.equals(requestToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getWriter(),
                    Result.fail(
                            CartCodeEnum.INTERNAL_ACCESS_DENIED.getCode(),
                            CartCodeEnum.INTERNAL_ACCESS_DENIED.getMessage()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
