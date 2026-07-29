package com.yirancrazy.minimall.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * Iter-3 OpenAPI/Knife4j 公共配置。各服务通过 {@code @SpringBootApplication}
 * 包扫描自动拾取。Knife4j UI 入口：{@code /doc.html}。
 */
@Configuration
public class OpenApiConfig
{

    @Bean
    public OpenAPI minimallOpenAPI()
    {
        return new OpenAPI()
            .info(new Info()
                .title("Minimall API")
                .description("薄荷商城后台系统 - 服务 API 文档")
                .version("1.0.0-SNAPSHOT")
                .contact(new Contact()
                    .name("yirancrazy")
                    .email("yirancrazy@gmail.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}