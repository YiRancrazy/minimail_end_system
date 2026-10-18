package com.yirancrazy.minimall.common.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MinioUtil 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class MinioUtil {

    private static final int EXPIRY_SECONDS = 900;


    private final MinioClient minioClient;


    private final MinioClient presignClient;

    private final String bucket;


    /**
     * 构造 MinioUtil 实例。
     * @param minioClient minioClient 参数
     * @param bucket bucket 参数
     */
    public MinioUtil(MinioClient minioClient, String bucket) {
        this(minioClient, bucket, null, null, null);
    }


    /**
     * 构造 MinioUtil 实例。
     * @param minioClient minioClient 参数
     * @param bucket bucket 参数
     * @param publicEndpoint publicEndpoint 参数
     * @param accessKey accessKey 参数
     * @param secretKey secretKey 参数
     */
    public MinioUtil(MinioClient minioClient, String bucket,
                     String publicEndpoint, String accessKey, String secretKey) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.presignClient = resolvePresignClient(minioClient, publicEndpoint, accessKey, secretKey);
    }

    /**
     * 转换或解析PresignClient。
     * @param minioClient minioClient
     * @param publicEndpoint publicEndpoint
     * @param accessKey accessKey
     * @param secretKey secretKey
     * @return 处理结果
     */
    private MinioClient resolvePresignClient(MinioClient minioClient, String publicEndpoint,
                                             String accessKey, String secretKey) {
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            return minioClient;
        }
        return MinioClient.builder()
            .endpoint(publicEndpoint)
            .credentials(accessKey, secretKey)
            .build();
    }


    /**
     * 执行 upload 方法。
     * @param inputStream inputStream 参数
     * @param size size 参数
     * @param contentType contentType 参数
     * @return 处理结果
     */
    public String upload(InputStream inputStream, long size, String contentType) {
        String objectKey = UUID.randomUUID().toString().replace("-", "");
        return upload(inputStream, objectKey, size, contentType);
    }


    /**
     * 执行 upload 方法。
     * @param inputStream inputStream 参数
     * @param objectKey objectKey 参数
     * @param size size 参数
     * @param contentType contentType 参数
     * @return 处理结果
     */
    public String upload(InputStream inputStream, String objectKey, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(inputStream, size, -1)
                .contentType(effectiveContentType(contentType, objectKey))
                .build());
            return objectKey;
        }
        catch (Exception e) {
            throw new BizException(CommonCode.SYS_ERROR,
                "MINIO_UPLOAD_FAIL",
                "上传文件到MinIO失败: " + e.getMessage());
        }
    }


    /**
     * 处理有效ContentType。
     * @param contentType contentType
     * @param objectKey objectKey
     * @return 处理结果
     */
    private static String effectiveContentType(String contentType, String objectKey) {
        String provided = contentType == null ? null : contentType.trim();
        boolean useless = provided == null || provided.isBlank()
            || "application/octet-stream".equalsIgnoreCase(provided);
        if (!useless) {
            return provided;
        }
        String key = objectKey.toLowerCase();
        if (key.endsWith(".webp")) return "image/webp";
        if (key.endsWith(".jpg") || key.endsWith(".jpeg")) return "image/jpeg";
        if (key.endsWith(".png")) return "image/png";
        if (key.endsWith(".gif")) return "image/gif";
        if (key.endsWith(".avif")) return "image/avif";
        if (key.endsWith(".bmp")) return "image/bmp";
        if (key.endsWith(".svg")) return "image/svg+xml";
        if (key.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }


    /**
     * 执行 upload 方法。
     * @param file file 参数
     * @param objectKey objectKey 参数
     * @param contentType contentType 参数
     * @return 处理结果
     */
    public String upload(Path file, String objectKey, String contentType) {
        // try-with-resources 确保文件流在上传结束（含异常路径）后被关闭，避免句柄泄漏
        try (InputStream in = Files.newInputStream(file)) {
            return upload(in, objectKey, Files.size(file), contentType);
        }
        catch (Exception e) {
            throw new BizException(CommonCode.SYS_ERROR,
                "MINIO_UPLOAD_FAIL",
                "上传文件到MinIO失败: " + e.getMessage());
        }
    }


    /**
     * 执行 presignedGetUrl 方法。
     * @param objectKey objectKey 参数
     * @return 处理结果
     */
    public String presignedGetUrl(String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(EXPIRY_SECONDS)
                    .build());
        }
        catch (Exception e) {
            throw new BizException(CommonCode.SYS_ERROR,
                "MINIO_PRESIGN_FAIL",
                "生成下载预签名URL失败: " + e.getMessage());
        }
    }


    /**
     * 执行 resolvePublicUrl 方法。
     * @param objectKey objectKey 参数
     * @return 处理结果
     */
    public String resolvePublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        return presignedGetUrl(objectKey);
    }
}
