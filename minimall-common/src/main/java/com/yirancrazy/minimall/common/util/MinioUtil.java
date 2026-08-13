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
 * @Description: MinIO 操作工具，支持文件流上传与预签名 URL 生成。
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
public class MinioUtil {

    private static final int EXPIRY_SECONDS = 900;

    private final MinioClient minioClient;

    private final String bucket;

    public MinioUtil(MinioClient minioClient, String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
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
                .contentType(contentType)
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
     * 上传本地文件到 MinIO 指定 objectKey。
     * @param file 本地文件路径
     * @param objectKey 目标对象 key
     * @param contentType 内容类型
     * @return objectKey
     * @throws BizException 当上传失败时
     */
    public String upload(Path file, String objectKey, String contentType) {
        try {
            return upload(Files.newInputStream(file), objectKey, Files.size(file), contentType);
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
            return minioClient.getPresignedObjectUrl(
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
}

