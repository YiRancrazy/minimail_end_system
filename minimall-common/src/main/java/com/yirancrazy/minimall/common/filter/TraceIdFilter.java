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
 * @Description: TraceIdFilter 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class TraceIdFilter extends OncePerRequestFilter {


    public static final String HEADER = "X-Trace-Id";

    /**
     * 执行 doFilterInternal 方法。
     * @param request request 参数
     * @param response response 参数
     * @param chain chain 参数
     * @throws ServletException 处理失败时抛出
     * @throws IOException 处理失败时抛出
     */
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
     * 处理sanitize。
     * @param raw raw
     * @return 处理结果
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
