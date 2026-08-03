package com.yirancrazy.minimall.goods.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Elasticsearch 配置类，启用 ES Repository 支持
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.yirancrazy.minimall.goods.search")
public class ElasticsearchConfig {
}
