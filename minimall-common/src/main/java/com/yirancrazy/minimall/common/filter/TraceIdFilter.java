package com.yirancrazy.minimall.common.filter;

import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 全链路 traceId 过滤器：从 X-Trace-Id 请求头取值（缺省生成）写入 MDC 并回写响应头，
 *               使服务日志 %X{traceId} 与 Result.traceId 贯通，便于按链路追踪排障。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** 链路追踪 ID 请求/响应头，与网关 TraceIdGlobalFilter.HEADER 保持一致 */
    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = sanitize(request.getHeader(HEADER));
        response.setHeader(HEADER, traceId);
        MDC.put(Result.TRACE_ID_KEY, traceId);
        try {
            chain.doFilter(request, response);
        }
        finally {
            MDC.remove(Result.TRACE_ID_KEY);
        }
    }

    /**
     * 规整客户端传入的 traceId：剔除 CR/LF 控制字符（防日志伪造），超过 64 字符时截断；为空时生成 UUID。
     * @param raw 请求头原始值，可为 null
     * @return 规整后的 traceId
     */
    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            // 直连服务（未经过网关）时自行生成，保证日志与响应均有 traceId
            return UUID.randomUUID().toString().replace("-", "");
        }
        String cleaned = raw.replace("\r", "").replace("\n", "");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}
