package com.yirancrazy.minimall.api.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 为内部 Feign 请求透传服务间访问令牌。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
public class InternalTokenFeignInterceptor implements RequestInterceptor {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final String internalToken;

    /**
     * 构造内部令牌拦截器。
     * @param internalToken 服务间访问令牌
     */
    public InternalTokenFeignInterceptor(String internalToken) {
        this.internalToken = internalToken;
    }

    /**
     * 向 Feign 请求写入内部令牌。
     * @param template Feign 请求模板
     */
    @Override
    public void apply(RequestTemplate template) {
        if (internalToken != null && !internalToken.isBlank()) {
            template.header(INTERNAL_TOKEN_HEADER, internalToken);
        }
    }
}
