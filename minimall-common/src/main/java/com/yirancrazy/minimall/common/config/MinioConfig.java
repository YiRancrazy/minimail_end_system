package com.yirancrazy.minimall.common.config;

import com.yirancrazy.minimall.common.service.ChunkUploadService;
import com.yirancrazy.minimall.common.util.MinioUtil;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 根据 minimall.minio.* 配置装配对象存储客户端、上传工具和分片上传服务。
 * 仅在配置了 endpoint 时启用，凭据和桶名由环境配置提供。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
     * 使用配置的内部 endpoint 和访问凭据创建 MinIO 客户端。
     *
     * @return MinIO 客户端实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    /**
     * 创建对象上传工具，并将公开 endpoint 与凭据传给预签名客户端配置。
     *
     * @return 绑定当前桶和预签名客户端的 MinioUtil
     */
    @Bean
    public MinioUtil minioUtil() {
        return new MinioUtil(minioClient(), bucket, publicEndpoint, accessKey, secretKey);
    }

    /**
     * 创建使用默认临时目录和上传限额的分片上传服务。
     *
     * @return 分片上传服务实例
     */
    @Bean
    public ChunkUploadService chunkUploadService() {
        return new ChunkUploadService(minioUtil());
    }
}
