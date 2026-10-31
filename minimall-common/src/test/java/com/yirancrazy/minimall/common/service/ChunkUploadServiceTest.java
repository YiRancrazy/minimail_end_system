package com.yirancrazy.minimall.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.util.MinioUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用临时目录、固定时钟和 Mock MinIO 验证分片上传的校验、限额、合并、清理和失败回滚。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class ChunkUploadServiceTest {

  private static final Long USER_ID = 42L;
  private static final long NANO_START = 0L;
  private static final ChunkUploadService.UploadLimits LIMITS =
      new ChunkUploadService.UploadLimits(3, 8, 12, 16, Duration.ofHours(1));

  @TempDir Path root;

  private MinioUtil minioUtil;
  private ChunkUploadService service;
  private String uploadId;

  /**
   * 测试目标：为每个用例准备独立临时根目录、固定限额、固定时钟和 MinIO Mock。
   * 测试场景与前置条件：JUnit 注入空临时目录，限额为 3 个分片、单分片 8 字节、任务 12 字节。
   * 关键步骤：创建服务并生成随机 uploadId。预期结果：用例之间文件状态隔离，时间固定。
   */
  @BeforeEach
  void setUp() {
    minioUtil = mock(MinioUtil.class);
    Clock clock = Clock.fixed(Instant.ofEpochSecond(NANO_START), ZoneOffset.UTC);
    service = new ChunkUploadService(minioUtil, root, LIMITS, clock);
    uploadId = UUID.randomUUID().toString();
  }

  /**
   * 创建测试用分片输入流。
   *
   * @param content 分片文本内容
   * @return 基于 UTF-8 内容的字节输入流
   */
  private InputStream chunk(String content) {
    return new ByteArrayInputStream(content.getBytes());
  }

  /** * 验证非法参数拒绝；空标识越界索引零总数或空用户；调用保存分片；预期均抛业务异常. */
  @Test
  void testSaveChunkParamInvalidThrows() {
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, "", 0, 2, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, uploadId, 2, 2, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, uploadId, -1, 2, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, uploadId, 0, 0, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(null, uploadId, 0, 1, "image/png", "a.png", chunk("x")));
    verify(minioUtil, never()).upload(any(InputStream.class), any(long.class), any(String.class));
  }

  /** * 验证恶意标识拒绝；输入路径穿越反斜杠短标识；调用保存分片；预期均拒绝且不落盘. */
  @Test
  void testSaveChunkMaliciousUploadIdRejected() {
    // 路径穿越/非法字符/过短 uploadId 一律在参数校验阶段拒绝，不得落盘或触碰 MinIO
    assertThrows(
        BizException.class,
        () ->
            service.saveChunk(USER_ID, "../../etc/passwd", 0, 1, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, "a\\b", 0, 1, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, "..", 0, 1, "image/png", "a.png", chunk("x")));
    assertThrows(
        BizException.class,
        () -> service.saveChunk(USER_ID, "abc", 0, 1, "image/png", "a.png", chunk("x")));
    verify(minioUtil, never()).upload(any(InputStream.class), any(long.class), any(String.class));
    verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
  }

  /** * 验证分片总数上限；总数超配置上限；调用保存分片；预期返回20035且不建目录. */
  @Test
  void testSaveChunkTotalChunksLimitExceededRejected() {
    BizException ex =
        assertThrows(
            BizException.class,
            () ->
                service.saveChunk(
                    USER_ID,
                    uploadId,
                    0,
                    LIMITS.maxTotalChunks() + 1,
                    "image/png",
                    "a.png",
                    chunk("x")));

    assertEquals("20035", ex.getCode());
    assertFalse(Files.exists(root.resolve(uploadId)));
  }

  /** * 验证单分片大小上限；提交九字节超过八字节；调用保存分片；预期返回20036且删除分片. */
  @Test
  void testSaveChunkSingleChunkLimitExceededRejected() {
    BizException ex =
        assertThrows(
            BizException.class,
            () ->
                service.saveChunk(
                    USER_ID, uploadId, 0, 1, "image/png", "a.png", chunk("123456789")));

    assertEquals("20036", ex.getCode());
    assertFalse(Files.exists(root.resolve(uploadId).resolve("chunk-0")));
    verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
  }

  /** * 验证任务总大小上限；先存八字节再存五字节；调用保存分片；预期返回20037且删除新分片. */
  @Test
  void testSaveChunkTaskTotalBytesLimitExceededRejected() {
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("12345678"));

    BizException ex =
        assertThrows(
            BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 1, 2, "image/png", "a.png", chunk("12345")));

    assertEquals("20037", ex.getCode());
    assertFalse(Files.exists(root.resolve(uploadId).resolve("chunk-1")));
  }

  /** * 验证用户总配额；两个任务共占十六字节；再追加一字节；预期返回20038且删除新分片. */
  @Test
  void testSaveChunkUserTotalBytesLimitExceededRejected() {
    String secondUploadId = UUID.randomUUID().toString();
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("12345678"));
    service.saveChunk(USER_ID, secondUploadId, 0, 2, "image/png", "b.png", chunk("12345678"));

    BizException ex =
        assertThrows(
            BizException.class,
            () ->
                service.saveChunk(USER_ID, secondUploadId, 1, 2, "image/png", "b.png", chunk("1")));

    assertEquals("20038", ex.getCode());
    assertFalse(Files.exists(root.resolve(secondUploadId).resolve("chunk-1")));
  }

  /** * 验证任务归属；用户42已建任务；用户43继续上传；预期返回20034. */
  @Test
  void testSaveChunkRejectsUploadIdOwnedByAnotherUser() {
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("x"));

    BizException ex =
        assertThrows(
            BizException.class,
            () -> service.saveChunk(USER_ID + 1, uploadId, 1, 2, "image/png", "a.png", chunk("y")));

    assertEquals("20034", ex.getCode());
  }

  /** * 验证单分片完成；文件名大写PNG；调用保存分片；预期完成且对象键扩展名小写. */
  @Test
  void testSaveChunkSingleChunkDoneUploads() {
    // 单分片任务收满后走合并上传路径，命中 Path 重载；后缀大写归一为小写
    when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
        .thenReturn(uploadId + ".png");
    ChunkUploadService.ChunkResult result =
        service.saveChunk(USER_ID, uploadId, 0, 1, "image/png", "avatar.PNG", chunk("data"));

    assertTrue(result.done());
    assertEquals(1, result.received());
    assertEquals(uploadId + ".png", result.objectKey());
    verify(minioUtil).upload(any(Path.class), eq(uploadId + ".png"), eq("image/png"));
  }

  /** * 验证无扩展名键；文件名为avatar；调用保存分片；预期对象键为原始上传标识. */
  @Test
  void testSaveChunkFileNameWithoutExtensionUsesPlainUploadId() {
    when(minioUtil.upload(any(Path.class), eq(uploadId), eq("image/png"))).thenReturn(uploadId);
    ChunkUploadService.ChunkResult result =
        service.saveChunk(USER_ID, uploadId, 0, 1, "image/png", "avatar", chunk("data"));

    assertTrue(result.done());
    assertEquals(uploadId, result.objectKey());
    verify(minioUtil).upload(any(Path.class), eq(uploadId), eq("image/png"));
  }

  /** * 验证未完成进度；总分片为二且只存第一片；调用保存分片；预期未完成且不上传. */
  @Test
  void testSaveChunkPartialReturnsNotDone() {
    ChunkUploadService.ChunkResult result =
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("hello"));

    assertFalse(result.done());
    assertEquals(1, result.received());
    verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
  }

  /** * 验证分片覆盖；同片先写abcdef再写xy；读取分片文件；预期内容仅为xy. */
  @Test
  void testSaveChunkRetryTruncatesExistingChunk() throws IOException {
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("abcdef"));
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("xy"));

    Path chunk = root.resolve(uploadId).resolve("chunk-0");
    assertEquals("xy", Files.readString(chunk));
  }

  /** * 验证最后分片合并；先存hello再存world；调用保存分片；预期完成后上传并清理目录. */
  @Test
  void testSaveChunkLastChunkTriggersMergeAndUpload() {
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("hello"));
    Path dir = root.resolve(uploadId);
    assertFalse(Files.exists(dir.resolve("merged.bin")), "未收满时不得提前合并");

    when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
        .thenReturn(uploadId + ".png");
    ChunkUploadService.ChunkResult result =
        service.saveChunk(USER_ID, uploadId, 1, 2, "image/png", "a.png", chunk("world"));

    assertTrue(result.done());
    assertEquals(2, result.received());
    verify(minioUtil).upload(any(Path.class), eq(uploadId + ".png"), eq("image/png"));
    // 上传后清理临时目录
    assertFalse(Files.isDirectory(dir));
  }

  /** * 验证进度排序；先上传第二片再上传第一片；查询进度；预期返回零和二. */
  @Test
  void testReceivedChunksListsUploadedChunksInOrder() {
    service.saveChunk(USER_ID, uploadId, 2, 3, "image/png", "a.png", chunk("c2"));
    service.saveChunk(USER_ID, uploadId, 0, 3, "image/png", "a.png", chunk("c0"));

    List<Integer> received = service.receivedChunks(USER_ID, uploadId, 3);
    assertEquals(List.of(0, 2), received);
  }

  /** * 验证未知任务进度；随机标识未创建；查询进度；预期返回空列表. */
  @Test
  void testReceivedChunksUnknownTaskReturnsEmpty() {
    assertTrue(service.receivedChunks(USER_ID, UUID.randomUUID().toString(), 3).isEmpty());
  }

  /** * 验证删除任务；任务目录已有分片；调用删除；预期目录移除. */
  @Test
  void testDeleteTaskRemovesTempDir() {
    service.saveChunk(USER_ID, uploadId, 0, 3, "image/png", "a.png", chunk("c0"));
    Path dir = root.resolve(uploadId);
    assertTrue(Files.isDirectory(dir));

    service.deleteTask(uploadId);
    assertFalse(Files.isDirectory(dir));
  }

  /** * 验证上传失败清理；最后分片时模拟上传失败；调用保存分片；预期异常保留且目录删除. */
  @Test
  void testMergeUploadFailurePropagatesAndCleansUp() {
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("hello"));
    // 模拟合并后上传 MinIO 失败，异常透出且临时目录仍被清理
    when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
        .thenThrow(new BizException(CommonCode.SYS_ERROR, "MINIO_UPLOAD_FAIL", "minio down"));

    BizException ex =
        assertThrows(
            BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 1, 2, "image/png", "a.png", chunk("world")));
    assertEquals("MINIO_UPLOAD_FAIL", ex.getAlias());
    assertFalse(Files.isDirectory(root.resolve(uploadId)));
  }

  /** * 验证过期清理；一个目录超过TTL一个活跃；调用清理；预期只删过期目录. */
  @Test
  void testCleanupExpiredTasksRemovesOnlyExpiredDirectories() throws IOException {
    service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("old"));
    Path expiredDir = root.resolve(uploadId);
    Files.setLastModifiedTime(
        expiredDir, FileTime.from(Instant.ofEpochSecond(NANO_START).minus(Duration.ofHours(2))));

    String activeUploadId = UUID.randomUUID().toString();
    service.saveChunk(USER_ID, activeUploadId, 0, 2, "image/png", "a.png", chunk("new"));

    int cleaned = service.cleanupExpiredTasks();

    assertEquals(1, cleaned);
    assertFalse(Files.exists(expiredDir));
    assertTrue(Files.isDirectory(root.resolve(activeUploadId)));
  }
}
