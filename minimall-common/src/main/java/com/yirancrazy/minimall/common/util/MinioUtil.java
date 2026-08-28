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
 * @Description: MinIO 操作工具，支持文件流上传与预签名 URL 生成；预签名 URL 可指定对外可达的 public-endpoint。
 * @Version: 1.2
 * @DateTime: 2026/08/05
 **/
public class MinioUtil {

    private static final int EXPIRY_SECONDS = 900;

    /** 内部连通用的 MinIO 客户端，负责上传文件流 */
    private final MinioClient minioClient;

    /** 对外下发图片用的客户端，endpoint 须为客户端可访问的公网/LAN 地址，用于生成预签名 URL */
    private final MinioClient presignClient;

    private final String bucket;

    /**
     * 兼容构造：未配置 public-endpoint 时，预签名 URL 复用内部客户端 endpoint。
     * @param minioClient 内部连通客户端
     * @param bucket MinIO 桶名
     */
    public MinioUtil(MinioClient minioClient, String bucket) {
        this(minioClient, bucket, null, null, null);
    }

    /**
     * 主构造：public-endpoint 非空时生成对外预签名 URL 用它作 host，否则退回内部客户端。
     * 预签名 URL 的 host 由构造客户端时的 endpoint 决定，公网主机必须能解析该地址，
     * 否则前端浏览器拿到 internal host（如 localdev）将无法显示图片。
     * @param minioClient 内部连通客户端
     * @param bucket MinIO 桶名
     * @param publicEndpoint 对外可访问的 MinIO endpoint；空值则退回内部客户端
     * @param accessKey 访问密钥 ID，public-endpoint 非空时需要
     * @param secretKey 访问密钥，public-endpoint 非空时需要
     */
    public MinioUtil(MinioClient minioClient, String bucket,
                     String publicEndpoint, String accessKey, String secretKey) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.presignClient = resolvePresignClient(minioClient, publicEndpoint, accessKey, secretKey);
    }

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
     * 上传文件流到 MinIO，objectKey 自动生成。
     * @param inputStream 文件流
     * @param size 文件大小（字节）
     * @param contentType 内容类型
     * @return 生成的 objectKey
     * @throws BizException 当上传失败时
     */
    public String upload(InputStream inputStream, long size, String contentType) {
        String objectKey = UUID.randomUUID().toString().replace("-", "");
        return upload(inputStream, objectKey, size, contentType);
    }

    /**
     * 上传文件流到 MinIO 指定 objectKey。
     * @param inputStream 文件流
     * @param objectKey 目标对象 key
     * @param size 文件大小（字节）
     * @param contentType 内容类型
     * @return objectKey
     * @throws BizException 当上传失败时
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
     * 兜底推断 MIME：上传方未带或只给了通用 octet-stream 时，按扩展名改写成具体 image/*，
     * 避免 MinIO 对象 Content-Type 失真导致 <img>/<picture>/Canvas 解析异常。
     * @param contentType 上传方提供的 Content-Type
     * @param objectKey 对象 key（含扩展名）
     * @return 优先使用显式类型；空/octet-stream 且扩展名可识别时返回对应 image 类型
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
     * 上传本地文件到 MinIO 指定 objectKey。
     * @param file 本地文件路径
     * @param objectKey 目标对象 key
     * @param contentType 内容类型
     * @return objectKey
     * @throws BizException 当上传失败时
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
     * 生成下载用的预签名 GET URL，有效期 15 分钟。
     * @param objectKey 目标对象 key
     * @return 预签名 URL 字符串
     * @throws BizException 当生成失败时
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
     * 把对外展示的对象 key 解析为可访问 URL：空值返回 null，已存为完整 URL 的旧数据原样返回，
     * 其余转预签名 URL。供各服务在把主图下发给前端时统一调用，避免裸 objectKey 导致图片不可显示。
     * @param objectKey MinIO 对象键或已存在的完整 URL
     * @return 可访问的图片 URL；空值返回 null
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

