package com.yirancrazy.minimall.common.util;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MinIO 对象上传、预签名下载与公开地址解析工具。
 * 可配置独立公开 endpoint 生成预签名 URL；对象键由调用方或上传方法决定，工具本身不校验业务归属。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class MinioUtil {

    private static final int EXPIRY_SECONDS = 900;

    private final MinioClient minioClient;

    private final MinioClient presignClient;

    private final String bucket;

    /**
     * 使用同一个客户端创建上传工具，不配置独立公开 endpoint。
     *
     * @param minioClient 用于上传和预签名的 MinIO 客户端
     * @param bucket 默认对象桶名
     */
    public MinioUtil(MinioClient minioClient, String bucket) {
        this(minioClient, bucket, null, null, null);
    }

    /**
     * 创建上传工具，并在配置公开 endpoint 时另建预签名客户端。
     *
     * @param minioClient 内部上传客户端
     * @param bucket 默认对象桶名
     * @param publicEndpoint 对外访问 endpoint；为空时复用内部客户端
     * @param accessKey 公开 endpoint 的访问凭据
     * @param secretKey 公开 endpoint 的访问密钥
     */
    public MinioUtil(
            MinioClient minioClient,
            String bucket,
            String publicEndpoint,
            String accessKey,
            String secretKey) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.presignClient = resolvePresignClient(minioClient, publicEndpoint, accessKey, secretKey);
    }

    /**
     * 当公开 endpoint 非空时创建独立的预签名客户端，否则复用内部客户端。
     *
     * @param minioClient 内部客户端
     * @param publicEndpoint 公开访问地址
     * @param accessKey 公开 endpoint 凭据
     * @param secretKey 公开 endpoint 密钥
     * @return 用于生成下载 URL 的客户端
     */
    private MinioClient resolvePresignClient(
            MinioClient minioClient, String publicEndpoint, String accessKey, String secretKey) {
        if (publicEndpoint == null || publicEndpoint.isBlank()) {
            return minioClient;
        }
        return MinioClient.builder().endpoint(publicEndpoint).credentials(accessKey, secretKey).build();
    }

    /**
     * 生成无短横线的 UUID 作为对象键并上传输入流。
     *
     * @param inputStream 待上传数据流
     * @param size 数据字节数
     * @param contentType 声明的内容类型
     * @return 生成的对象键
     */
    public String upload(InputStream inputStream, long size, String contentType) {
        String objectKey = UUID.randomUUID().toString().replace("-", "");
        return upload(inputStream, objectKey, size, contentType);
    }

    /**
     * 将输入流上传到指定对象键，内容类型为空或 octet-stream 时按扩展名推断。
     *
     * @param inputStream 待上传数据流
     * @param objectKey 目标对象键
     * @param size 数据字节数
     * @param contentType 声明的内容类型
     * @return 已上传的对象键
     * @throws BizException 上传失败时抛出
     */
    public String upload(InputStream inputStream, String objectKey, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(inputStream, size, -1)
                            .contentType(effectiveContentType(contentType, objectKey))
                            .build());
            return objectKey;
        } catch (Exception e) {
            throw new BizException(
                    CommonCode.SYS_ERROR, "MINIO_UPLOAD_FAIL", "上传文件到MinIO失败: " + e.getMessage());
        }
    }

    /**
     * 打开本地文件并上传，使用 try-with-resources 确保异常路径也关闭文件流。
     *
     * @param file 本地文件路径
     * @param objectKey 目标对象键
     * @param contentType 声明的内容类型
     * @return 已上传的对象键
     * @throws BizException 文件读取或上传失败时抛出
     */
    public String upload(Path file, String objectKey, String contentType) {
        // try-with-resources 确保文件流在上传结束（含异常路径）后被关闭，避免句柄泄漏
        try (InputStream in = Files.newInputStream(file)) {
            return upload(in, objectKey, Files.size(file), contentType);
        } catch (Exception e) {
            throw new BizException(
                    CommonCode.SYS_ERROR, "MINIO_UPLOAD_FAIL", "上传文件到MinIO失败: " + e.getMessage());
        }
    }

    /**
     * 优先保留有效的显式内容类型；缺失或为 octet-stream 时按常见图片扩展名推断。
     *
     * @param contentType 调用方传入的内容类型
     * @param objectKey 对象键，用于提取扩展名
     * @return 最终写入 MinIO 的内容类型
     */
    private static String effectiveContentType(String contentType, String objectKey) {
        String provided = contentType == null ? null : contentType.trim();
        boolean useless =
                provided == null
                        || provided.isBlank()
                        || "application/octet-stream".equalsIgnoreCase(provided);
        if (!useless) {
            return provided;
        }
        String key = objectKey.toLowerCase();
        if (key.endsWith(".webp")) {
            return "image/webp";
        }
        if (key.endsWith(".jpg") || key.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (key.endsWith(".png")) {
            return "image/png";
        }
        if (key.endsWith(".gif")) {
            return "image/gif";
        }
        if (key.endsWith(".avif")) {
            return "image/avif";
        }
        if (key.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (key.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (key.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    /**
     * 为对象生成有效期为 15 分钟的 GET 预签名 URL。
     *
     * @param objectKey 目标对象键
     * @return 可直接下载对象的预签名 URL
     * @throws BizException 生成 URL 失败时抛出
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
        } catch (Exception e) {
            throw new BizException(
                    CommonCode.SYS_ERROR, "MINIO_PRESIGN_FAIL", "生成下载预签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 将对象键解析为可访问 URL；已是 HTTP(S) 地址时原样返回，空值返回 null。
     *
     * @param objectKey 对象键或完整 HTTP(S) URL
     * @return 可访问 URL 或 null
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
