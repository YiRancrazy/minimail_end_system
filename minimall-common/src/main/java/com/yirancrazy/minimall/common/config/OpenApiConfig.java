package com.yirancrazy.minimall.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 OpenAPI 文档的标题、版本、联系人、许可证等元信息，
 * 供各服务复用统一的接口文档入口配置。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Configuration
public class OpenApiConfig {

    /**
     * 构建薄荷商城 API 文档元信息。
     *
     * @return 包含项目标题、版本、联系人和许可证的 OpenAPI 模型
     */
    @Bean
    public OpenAPI minimallOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Minimall API")
                                .description("薄荷商城后台系统 - 服务 API 文档")
                                .version("1.0.0-SNAPSHOT")
                                .contact(new Contact().name("yirancrazy").email("yirancrazy@gmail.com"))
                                .license(
                                        new License()
                                                .name("Apache 2.0")
                                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
