package com.yirancrazy.minimall.api.config;

import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将当前 MDC 链路标识写入 Feign 请求头，贯通跨服务调用链.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class TraceIdFeignInterceptor implements RequestInterceptor {

    /**
     * 将当前非空 MDC TraceId 写入 Feign 请求头.
     *
     * @param template 待补充请求头的 Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            template.header(TraceIdFilter.HEADER, traceId);
        }
    }
}
