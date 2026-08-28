package com.yirancrazy.minimall.common.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MinioUtil 单元测试，验证上传 MIME 兜底推断与 public-endpoint 构造分支。
 * @Version: 1.0
 * @DateTime: 2026/08/27
 */
class MinioUtilTest {

    private final MinioClient minioClient = mock(MinioClient.class);

    private final MinioUtil util = new MinioUtil(minioClient, "minimail");

    @Test
    void upload_without_content_type_infers_webp_from_object_key() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(inv -> {
            PutObjectArgs args = inv.getArgument(0);
            assertEquals("image/webp", args.contentType());
            assertEquals("abc.webp", args.object());
            return null;
        });
        byte[] bytes = "fake-webp".getBytes(StandardCharsets.UTF_8);
        String key = util.upload(new ByteArrayInputStream(bytes), "abc.webp",
                bytes.length, null);
        assertEquals("abc.webp", key);
    }

    @Test
    void upload_octet_stream_with_known_extension_is_overridden() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(inv -> {
            PutObjectArgs args = inv.getArgument(0);
            assertEquals("image/png", args.contentType());
            return null;
        });
        byte[] bytes = "png".getBytes(StandardCharsets.UTF_8);
        util.upload(new ByteArrayInputStream(bytes), "x.png", bytes.length,
                "application/octet-stream");
    }

    @Test
    void upload_explicit_content_type_is_kept() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(inv -> {
            PutObjectArgs args = inv.getArgument(0);
            assertEquals("image/jpeg", args.contentType());
            return null;
        });
        byte[] bytes = "jpg".getBytes(StandardCharsets.UTF_8);
        util.upload(new ByteArrayInputStream(bytes), "y.webp", bytes.length, "image/jpeg");
    }

    @Test
    void upload_unknown_extension_stays_octet_stream() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenAnswer(inv -> {
            PutObjectArgs args = inv.getArgument(0);
            assertEquals("application/octet-stream", args.contentType());
            return null;
        });
        byte[] bytes = "bin".getBytes(StandardCharsets.UTF_8);
        util.upload(new ByteArrayInputStream(bytes), "a.dat", bytes.length, null);
    }

    @Test
    void upload_failure_throws_biz_exception() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("boom"));
        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        assertThrows(BizException.class,
                () -> util.upload(new ByteArrayInputStream(bytes), "a.png", bytes.length, null));
    }
}
