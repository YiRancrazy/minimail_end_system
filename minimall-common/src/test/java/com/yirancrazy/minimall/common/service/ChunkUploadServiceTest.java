package com.yirancrazy.minimall.common.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private static final Path ROOT = Paths.get(
        System.getProperty("java.io.tmpdir"), "mm-chunk");

    private MinioUtil minioUtil;
    private ChunkUploadService service;
    private String uploadId;

    @BeforeEach
    void setUp() {
        minioUtil = mock(MinioUtil.class);
        service = new ChunkUploadService(minioUtil);
        uploadId = UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() throws IOException {
        service.deleteTask(uploadId);
    }

    private InputStream chunk(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Test
    void saveChunk_param_invalid_throws() {
        assertThrows(BizException.class,
            () -> service.saveChunk("", 0, 2, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(uploadId, 2, 2, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(uploadId, -1, 2, "image/png", "a.png", chunk("x")));
        assertThrows(BizException.class,
            () -> service.saveChunk(uploadId, 0, 0, "image/png", "a.png", chunk("x")));
        verify(minioUtil, never()).upload(any(InputStream.class), any(long.class), any(String.class));
    }

    @Test
    void saveChunk_single_chunk_done_uploads() {
        // 单分片任务收满后走合并上传路径，命中 Path 重载；后缀大写归一为小写
        when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
            .thenReturn(uploadId + ".png");
        ChunkUploadService.ChunkResult result =
            service.saveChunk(uploadId, 0, 1, "image/png", "avatar.PNG", chunk("data"));

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
            service.saveChunk(uploadId, 0, 1, "image/png", "avatar", chunk("data"));

        assertTrue(result.done());
        assertEquals(uploadId, result.objectKey());
        verify(minioUtil).upload(any(Path.class), eq(uploadId), eq("image/png"));
    }

    @Test
    void saveChunk_partial_returns_not_done() {
        ChunkUploadService.ChunkResult result =
            service.saveChunk(uploadId, 0, 2, "image/png", "a.png", chunk("hello"));

        assertFalse(result.done());
        assertEquals(1, result.received());
        verify(minioUtil, never()).upload(any(Path.class), any(String.class), any(String.class));
    }

    @Test
    void saveChunk_last_chunk_triggers_merge_and_upload() {
        service.saveChunk(uploadId, 0, 2, "image/png", "a.png", chunk("hello"));
        Path dir = ROOT.resolve(uploadId);
        assertFalse(Files.exists(dir.resolve("merged.bin")), "未收满时不得提前合并");

        when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
            .thenReturn(uploadId + ".png");
        ChunkUploadService.ChunkResult result =
            service.saveChunk(uploadId, 1, 2, "image/png", "a.png", chunk("world"));

        assertTrue(result.done());
        assertEquals(2, result.received());
        verify(minioUtil).upload(any(Path.class), eq(uploadId + ".png"), eq("image/png"));
        // 上传后清理临时目录
        assertFalse(Files.isDirectory(dir));
    }

    @Test
    void receivedChunks_lists_uploaded_chunks_in_order() {
        service.saveChunk(uploadId, 2, 4, "image/png", "a.png", chunk("c2"));
        service.saveChunk(uploadId, 0, 4, "image/png", "a.png", chunk("c0"));

        List<Integer> received = service.receivedChunks(uploadId, 4);
        assertEquals(List.of(0, 2), received);
    }

    @Test
    void receivedChunks_unknown_task_returns_empty() {
        assertTrue(service.receivedChunks(UUID.randomUUID().toString(), 3).isEmpty());
    }

    @Test
    void deleteTask_removes_temp_dir() {
        service.saveChunk(uploadId, 0, 3, "image/png", "a.png", chunk("c0"));
        Path dir = ROOT.resolve(uploadId);
        assertTrue(Files.isDirectory(dir));

        service.deleteTask(uploadId);
        assertFalse(Files.isDirectory(dir));
    }

    @Test
    void merge_upload_failure_propagates_and_cleans_up() {
        service.saveChunk(uploadId, 0, 2, "image/png", "a.png", chunk("hello"));
        // 模拟合并后上传 MinIO 失败，异常透出且临时目录仍被清理
        when(minioUtil.upload(any(Path.class), eq(uploadId + ".png"), eq("image/png")))
            .thenThrow(new BizException(CommonCode.SYS_ERROR, "MINIO_UPLOAD_FAIL", "minio down"));

        BizException ex = assertThrows(BizException.class,
            () -> service.saveChunk(uploadId, 1, 2, "image/png", "a.png", chunk("world")));
        assertEquals("MINIO_UPLOAD_FAIL", ex.getAlias());
        assertFalse(Files.isDirectory(ROOT.resolve(uploadId)));
    }
}
