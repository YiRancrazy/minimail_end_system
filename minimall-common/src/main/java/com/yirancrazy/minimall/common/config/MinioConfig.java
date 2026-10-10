package com.yirancrazy.minimall.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;
import com.yirancrazy.minimall.common.service.ChunkUploadService;
import com.yirancrazy.minimall.common.util.MinioUtil;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MinIO 自动装配，当 minimall.minio.endpoint 配置存在时注册 MinioClient 与 MinioUtil。
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

    @Value("${minimall.minio.public-endpoint:}")
    private String publicEndpoint;

    /**
     * 构建用于对象存储操作的 MinioClient Bean。
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }

    /**
     * 构建用于预签名 URL 生成的 MinioUtil Bean。
     * @return MinioUtil 实例
     */
    @Bean
    public MinioUtil minioUtil() {
        return new MinioUtil(minioClient(), bucket, publicEndpoint, accessKey, secretKey);
    }

    /**
     * 构建用于分片上传支持的 ChunkUploadService Bean。
     * @return ChunkUploadService 实例
     */
    @Bean
    public ChunkUploadService chunkUploadService() {
        return new ChunkUploadService(minioUtil());
    }
}
