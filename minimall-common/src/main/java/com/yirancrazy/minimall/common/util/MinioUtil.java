package com.yirancrazy.minimall.common.util;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MinIO 预签名 URL 工具，生成上传与下载的临时访问地址。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Component
public class MinioUtil {

    private static final int EXPIRY_SECONDS = 900;

    private final MinioClient minioClient;

    @Value("${minimall.minio.bucket:mall-files}")
    private String bucket;

    public MinioUtil(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 生成上传用的预签名 PUT URL，有效期 15 分钟。
     * @param objectKey 目标对象 key，若为空则自动生成
     * @return 预签名 URL 字符串
     * @throws BizException 当生成失败时
     */
    public String presignedPutUrl(String objectKey) {
        String key = (objectKey == null || objectKey.isBlank())
            ? UUID.randomUUID().toString().replace("-", "")
            : objectKey;
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(key)
                    .expiry(EXPIRY_SECONDS)
                    .build());
        }
        catch (Exception e) {
            throw new BizException(CommonCode.SYS_ERROR,
                "MINIO_PRESIGN_FAIL",
                "生成上传预签名URL失败: " + e.getMessage());
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

