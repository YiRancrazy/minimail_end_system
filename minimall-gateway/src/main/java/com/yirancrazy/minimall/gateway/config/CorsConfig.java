package com.yirancrazy.minimall.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CorsConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Configuration
public class CorsConfig {

    /**
     * 执行 corsWebFilter 方法。
     * @return 处理结果
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 携带凭据的 CORS 不允许通配 "*"，仅放行三端前端 dev 端口（user-web/merchant-admin/platform-admin）
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(src);
    }
}
