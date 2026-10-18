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
 * @Description: MinioConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * 执行 minioClient 方法。
     * @return 处理结果
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }


    /**
     * 执行 minioUtil 方法。
     * @return 处理结果
     */
    @Bean
    public MinioUtil minioUtil() {
        return new MinioUtil(minioClient(), bucket, publicEndpoint, accessKey, secretKey);
    }


    /**
     * 执行 chunkUploadService 方法。
     * @return 处理结果
     */
    @Bean
    public ChunkUploadService chunkUploadService() {
        return new ChunkUploadService(minioUtil());
    }
}
