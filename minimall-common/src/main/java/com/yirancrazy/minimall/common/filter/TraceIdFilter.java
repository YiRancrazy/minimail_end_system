package com.yirancrazy.minimall.common.filter;

import com.yirancrazy.minimall.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Servlet 请求链路标识过滤器，优先复用 X-Trace-Id 请求头，缺失时生成新标识，
 * 写入响应头和 MDC，并在请求结束后清理线程上下文。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Trace-Id";

  /**
   * 为当前请求建立 traceId 上下文，执行后续过滤器链，并保证 finally 清理 MDC。
   *
   * @param request 当前 HTTP 请求
   * @param response 当前 HTTP 响应
   * @param chain 后续 Servlet 过滤器链
   * @throws ServletException 后续过滤器抛出 Servlet 异常时原样传播
   * @throws IOException 读写请求或响应失败时原样传播
   */
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = sanitize(request.getHeader(HEADER));
    response.setHeader(HEADER, traceId);
    MDC.put(Result.TRACE_ID_KEY, traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(Result.TRACE_ID_KEY);
    }
  }

  /**
   * 清理外部传入的 traceId：移除换行符，超长值截断到 64 字符，空值生成 UUID。
   *
   * @param raw 原始 X-Trace-Id 请求头
   * @return 可安全写入响应和日志的 traceId
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
