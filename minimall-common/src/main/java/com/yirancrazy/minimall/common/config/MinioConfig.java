package com.yirancrazy.minimall.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MinIO 客户端配置，当 minimall.minio.endpoint 配置存在时自动装配。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Configuration
@ConditionalOnProperty(name = "minimall.minio.endpoint")
public class MinioConfig {

    @Value("${minimall.minio.endpoint}")
    private String endpoint;

    @Value("${minimall.minio.access-key}")
    private String accessKey;

    @Value("${minimall.minio.secret-key}")
    private String secretKey;

    @Value("${minimall.minio.bucket:mall-files}")
    private String bucket;

    /**
     * Build MinioClient bean for object storage operations.
     * @return MinioClient instance
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }

    /**
     * Get default bucket name.
     * @return bucket name
     */
    public String getBucket() {
        return bucket;
    }
}
