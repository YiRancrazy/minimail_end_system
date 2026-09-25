package com.yirancrazy.minimall.common.service;

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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ChunkUploadService 单元测试，验证分片落盘、收满合并上传 MinIO、断点查询与参数校验。
 * @Version: 1.0
 * @DateTime: 2026/08/12
 **/
class ChunkUploadServiceTest {

    private static final Long USER_ID = 42L;
    private static final long NANO_START = 0L;
    private static final ChunkUploadService.UploadLimits LIMITS =
        new ChunkUploadService.UploadLimits(
        3, 8, 12, 16, Duration.ofHours(1));

    @TempDir
    Path root;

    private MinioUtil minioUtil;
    private ChunkUploadService service;
    private String uploadId;

    @BeforeEach
    void setUp() {
        minioUtil = mock(MinioUtil.class);
        Clock clock = Clock.fixed(Instant.ofEpochSecond(NANO_START), ZoneOffset.UTC);
        service = new ChunkUploadService(minioUtil, root, LIMITS, clock);
        uploadId = UUID.randomUUID().toString();
    }

    private InputStream chunk(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Test
    void saveChunk_param_invalid_throws() {
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, "", 0, 2, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 2, 2, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, -1, 2, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 0, 0, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(null, uploadId, 0, 1, "image/png", "a.png", chunk("x")));
        verify(minioUtil, never()).upload(any(InputStream.class), any(long.class), any(String.class));
    }

    @Test
    void saveChunk_malicious_uploadId_rejected() {
        // 路径穿越/非法字符/过短 uploadId 一律在参数校验阶段拒绝，不得落盘或触碰 MinIO
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, "../../etc/passwd", 0, 1,
                "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, "a\\b", 0, 1, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, "..", 0, 1, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, "abc", 0, 1, "image/png", "a.png", chunk("x")));
        verify(minioUtil, never()).upload(any(InputStream.class), any(long.class), any(String.class));
        verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
    }

    @Test
    void saveChunk_total_chunks_limit_exceeded_rejected() {
        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 0, LIMITS.maxTotalChunks() + 1,
                "image/png", "a.png", chunk("x")));

        assertEquals("20035", ex.getCode());
        assertFalse(Files.exists(root.resolve(uploadId)));
    }

    @Test
    void saveChunk_single_chunk_limit_exceeded_rejected() {
        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 0, 1, "image/png", "a.png",
                chunk("123456789")));

        assertEquals("20036", ex.getCode());
        assertFalse(Files.exists(root.resolve(uploadId).resolve("chunk-0")));
        verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
    }

    @Test
    void saveChunk_task_total_bytes_limit_exceeded_rejected() {
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("12345678"));

        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 1, 2, "image/png", "a.png",
                chunk("12345")));

        assertEquals("20037", ex.getCode());
        assertFalse(Files.exists(root.resolve(uploadId).resolve("chunk-1")));
    }

    @Test
    void saveChunk_user_total_bytes_limit_exceeded_rejected() {
        String secondUploadId = UUID.randomUUID().toString();
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("12345678"));
        service.saveChunk(USER_ID, secondUploadId, 0, 2, "image/png", "b.png", chunk("12345678"));

        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, secondUploadId, 1, 2, "image/png", "b.png",
                chunk("1")));

        assertEquals("20038", ex.getCode());
        assertFalse(Files.exists(root.resolve(secondUploadId).resolve("chunk-1")));
    }

    @Test
    void saveChunk_rejects_upload_id_owned_by_another_user() {
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("x"));

        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID + 1, uploadId, 1, 2,
                "image/png", "a.png", chunk("y")));

        assertEquals("20034", ex.getCode());
    }

    @Test
    void saveChunk_single_chunk_done_uploads() {
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

    @Test
    void saveChunk_fileName_without_extension_uses_plain_uploadId() {
        when(minioUtil.upload(any(Path.class), eq(uploadId), eq("image/png")))
            .thenReturn(uploadId);
        ChunkUploadService.ChunkResult result =
            service.saveChunk(USER_ID, uploadId, 0, 1, "image/png", "avatar", chunk("data"));

        assertTrue(result.done());
        assertEquals(uploadId, result.objectKey());
        verify(minioUtil).upload(any(Path.class), eq(uploadId), eq("image/png"));
    }

    @Test
    void saveChunk_partial_returns_not_done() {
        ChunkUploadService.ChunkResult result =
            service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("hello"));

        assertFalse(result.done());
        assertEquals(1, result.received());
        verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
    }

    @Test
    void saveChunk_retry_truncates_existing_chunk() throws IOException {
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("abcdef"));
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("xy"));

        Path chunk = root.resolve(uploadId).resolve("chunk-0");
        assertEquals("xy", Files.readString(chunk));
    }

    @Test
    void saveChunk_last_chunk_triggers_merge_and_upload() {
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

    @Test
    void receivedChunks_lists_uploaded_chunks_in_order() {
        service.saveChunk(USER_ID, uploadId, 2, 3, "image/png", "a.png", chunk("c2"));
        service.saveChunk(USER_ID, uploadId, 0, 3, "image/png", "a.png", chunk("c0"));

        List<Integer> received = service.receivedChunks(USER_ID, uploadId, 3);
        assertEquals(List.of(0, 2), received);
    }

    @Test
    void receivedChunks_unknown_task_returns_empty() {
        assertTrue(service.receivedChunks(USER_ID, UUID.randomUUID().toString(), 3).isEmpty());
    }

    @Test
    void deleteTask_removes_temp_dir() {
        service.saveChunk(USER_ID, uploadId, 0, 3, "image/png", "a.png", chunk("c0"));
        Path dir = root.resolve(uploadId);
        assertTrue(Files.isDirectory(dir));

        service.deleteTask(uploadId);
        assertFalse(Files.isDirectory(dir));
    }

    @Test
    void merge_upload_failure_propagates_and_cleans_up() {
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("hello"));
        // 模拟合并后上传 MinIO 失败，异常透出且临时目录仍被清理
        when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
            .thenThrow(new BizException(CommonCode.SYS_ERROR, "MINIO_UPLOAD_FAIL", "minio down"));

        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(USER_ID, uploadId, 1, 2,
                "image/png", "a.png", chunk("world")));
        assertEquals("MINIO_UPLOAD_FAIL", ex.getAlias());
        assertFalse(Files.isDirectory(root.resolve(uploadId)));
    }

    @Test
    void cleanupExpiredTasks_removes_only_expired_directories() throws IOException {
        service.saveChunk(USER_ID, uploadId, 0, 2, "image/png", "a.png", chunk("old"));
        Path expiredDir = root.resolve(uploadId);
        Files.setLastModifiedTime(expiredDir,
            FileTime.from(Instant.ofEpochSecond(NANO_START).minus(Duration.ofHours(2))));

        String activeUploadId = UUID.randomUUID().toString();
        service.saveChunk(USER_ID, activeUploadId, 0, 2, "image/png", "a.png", chunk("new"));

        int cleaned = service.cleanupExpiredTasks();

        assertEquals(1, cleaned);
        assertFalse(Files.exists(expiredDir));
        assertTrue(Files.isDirectory(root.resolve(activeUploadId)));
    }
}
