package com.yirancrazy.minimall.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.exception.BizException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mock MinioClient 验证上传内容类型推断、显式类型保留和失败异常转换。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MinioUtilTest {

  private final MinioClient minioClient = mock(MinioClient.class);

  private final MinioUtil util = new MinioUtil(minioClient, "minimail");

  /** * 验证类型推断；上传webp且类型为空；检查上传参数；预期类型为image/webp. */
  @Test
  void testUploadWithoutContentTypeInfersWebpFromObjectKey() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenAnswer(
            inv -> {
              PutObjectArgs args = inv.getArgument(0);
              assertEquals("image/webp", args.contentType());
              assertEquals("abc.webp", args.object());
              return null;
            });
    byte[] bytes = "fake-webp".getBytes(StandardCharsets.UTF_8);
    String key = util.upload(new ByteArrayInputStream(bytes), "abc.webp", bytes.length, null);
    assertEquals("abc.webp", key);
  }

  /** * 验证二进制流覆盖；上传png且类型为二进制流；检查上传参数；预期类型为image/png. */
  @Test
  void testUploadOctetStreamWithKnownExtensionIsOverridden() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenAnswer(
            inv -> {
              PutObjectArgs args = inv.getArgument(0);
              assertEquals("image/png", args.contentType());
              return null;
            });
    byte[] bytes = "png".getBytes(StandardCharsets.UTF_8);
    util.upload(new ByteArrayInputStream(bytes), "x.png", bytes.length, "application/octet-stream");
  }

  /** * 验证显式类型优先；webp对象传jpeg类型；检查上传参数；预期仍为image/jpeg. */
  @Test
  void testUploadExplicitContentTypeIsKept() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenAnswer(
            inv -> {
              PutObjectArgs args = inv.getArgument(0);
              assertEquals("image/jpeg", args.contentType());
              return null;
            });
    byte[] bytes = "jpg".getBytes(StandardCharsets.UTF_8);
    util.upload(new ByteArrayInputStream(bytes), "y.webp", bytes.length, "image/jpeg");
  }

  /** * 验证未知扩展名；上传dat且类型为空；检查上传参数；预期回退二进制流. */
  @Test
  void testUploadUnknownExtensionStaysOctetStream() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenAnswer(
            inv -> {
              PutObjectArgs args = inv.getArgument(0);
              assertEquals("application/octet-stream", args.contentType());
              return null;
            });
    byte[] bytes = "bin".getBytes(StandardCharsets.UTF_8);
    util.upload(new ByteArrayInputStream(bytes), "a.dat", bytes.length, null);
  }

  /** * 验证上传失败；底层客户端抛运行异常；调用上传；预期抛业务异常. */
  @Test
  void testUploadFailureThrowsBizException() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("boom"));
    byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
    assertThrows(
        BizException.class,
        () -> util.upload(new ByteArrayInputStream(bytes), "a.png", bytes.length, null));
  }
}
