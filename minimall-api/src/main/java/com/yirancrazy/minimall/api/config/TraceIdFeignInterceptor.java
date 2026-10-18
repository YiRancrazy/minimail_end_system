package com.yirancrazy.minimall.api.config;

import org.slf4j.MDC;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdFeignInterceptor 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class TraceIdFeignInterceptor implements RequestInterceptor {


    /**
     * 执行 apply 方法。
     * @param template template 参数
     */
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            template.header(TraceIdFilter.HEADER, traceId);
        }
    }
}
