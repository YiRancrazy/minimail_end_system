package com.yirancrazy.minimall.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 为用户端、商家端和平台端浏览器配置网关级响应式 CORS。该配置仅允许文档中列出的
 *   本地开发源发起携带凭据的请求，JWT 鉴权仍由 AuthGlobalFilter 负责。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Configuration
public class CorsConfig {

  /**
   * 为全部网关路径构建 CORS 过滤器。
   *
   * <p>策略允许 localhost 和 127.0.0.1 的 5173-5175 端口携带凭据访问，允许常用 HTTP 方法和全部
   * 请求头。过滤器在下游处理器之前生效，但不负责请求鉴权。
   *
   * @return 注册到全部路径的响应式 CORS 过滤器
   */
  @Bean
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration cfg = new CorsConfiguration();
    // 携带凭据的 CORS 不允许通配 "*"，仅放行三端前端 dev 端口（user-web/merchant-admin/platform-admin）
    cfg.setAllowedOriginPatterns(
        List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://127.0.0.1:5175"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return new CorsWebFilter(src);
  }
}
