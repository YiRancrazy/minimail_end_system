package com.yirancrazy.minimall.api.config;

import org.slf4j.MDC;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Feign 出站请求 traceId 透传拦截器：将当前线程 MDC 中的 traceId 写入 X-Trace-Id 请求头，
 *               使被调服务的 TraceIdFilter 沿用同一链路 ID，保证跨服务日志可串联。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
public class TraceIdFeignInterceptor implements RequestInterceptor {

    /**
     * 为每个 Feign 请求注入当前链路 traceId；MDC 无值时不加头，交由对端自行生成。
     * @param template 待发送的 Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            template.header(TraceIdFilter.HEADER, traceId);
        }
    }
}
